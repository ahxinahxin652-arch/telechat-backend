/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午11:26
 */
package com.telechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.telechat.constant.ExceptionConstant;
import com.telechat.constant.RedisConstant;
import com.telechat.exception.exceptions.ContactException;
import com.telechat.mapper.dao.ContactDao;
import com.telechat.mapper.dao.ConversationMemberDao;
import com.telechat.mapper.dao.UserDao;
import com.telechat.pojo.cache.ContactsCache;
import com.telechat.pojo.cache.UserInfoCache;
import com.telechat.pojo.dto.contact.UpdateContactDTO;
import com.telechat.pojo.entity.Contact;
import com.telechat.pojo.entity.ConversationMember;
import com.telechat.pojo.entity.User;
import com.telechat.pojo.vo.ContactVO;
import com.telechat.service.ContactService;
import com.telechat.util.RedisTemplateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactDao contactDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ConversationMemberDao conversationMemberDao;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisTemplateUtil redisTemplateUtil;

    /**
     * 获取联系人列表
     *
     * @param userId 用户ID
     * @return List<ContactVO>
     */
    @Override// todo 分页获取请求
    public List<ContactVO> list(Long userId) {
        // 1. 获取好友关系列表 (从 Redis，仅包含 IDs)
        List<ContactsCache> relationCaches = redisTemplateUtil.getContactCache(userId);

        if (relationCaches.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取所有好友 ID
        Set<Long> friendIds = relationCaches.stream()
                .map(ContactsCache::getFriendId)
                .collect(Collectors.toSet());

        // 3. 【核心】批量获取好友的详细信息 (UserInfoCache)
        // 这一步利用了 Redis 的 MultiGet，速度极快
        Map<Long, UserInfoCache> userInfoMap = redisTemplateUtil.getUserInfoCacheMapByIds(friendIds);

        // 4. 组装最终 VO
        return relationCaches.stream().map(relation -> {
            UserInfoCache info = userInfoMap.get(relation.getFriendId());

            // 处理 info 可能为 null 的情况（例如用户注销了，或者数据库脏数据）
            String nickname = (info != null) ? info.getNickname() : null;
            String avatar = (info != null) ? info.getAvatar() : null;
            String username = (info != null) ? info.getUsername() : null;
            String bio = (info != null) ? info.getBio() : null;

            return ContactVO.builder()
                    .id(relation.getContactId())
                    .userId(relation.getFriendId())
                    .remark(relation.getRemark()) // 备注来自关系缓存
                    .nickname(nickname)           // 昵称来自实体缓存
                    .username(username)
                    .avatar(avatar)
                    .bio(bio)                     // 签名来自实体缓存
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 删除联系人
     *
     * @param id     联系人ID
     * @param userId 用户ID
     * @return boolean
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean delete(Long id, Long userId) {
        // 1. 获取并校验联系人记录
        Contact contact = contactDao.selectById(id);
        if (contact == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.CONTACT_NOT_EXIST_MSG);
        }
        // 不能删除别人的联系人
        if (!contact.getUserId().equals(userId)) {
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE, ExceptionConstant.NOT_ALLOWED_MSG);
        }

        // 2. Fail-Fast: 提前校验会话ID，避免走到一半才抛异常回滚
        Long conversationId = contact.getConversationId();
        if (conversationId == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.CONVERSATION_NOT_EXIST_MSG);
        }

        // 3. 执行删除联系人
        contactDao.delete(contact.getId());

        // 4. 性能优化：无需 select，直接 update 对应记录的状态
        ConversationMember conversationMember = ConversationMember.builder()
                .conversationId(conversationId)
                .userId(userId)
                .isDeleted(true)
                .build();
        boolean memberUpdated = conversationMemberDao.updateSettings(conversationMember) > 0;

        if (!memberUpdated) {
            log.warn("Contact delete: Conversation member not found or already deleted. convId: {}, userId: {}", conversationId, userId);
        }

        // 5. 缓存清理：利用事务同步管理器，确保【数据库事务提交成功后】再清理缓存
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTemplateUtil.deleteContactCache(userId);
                    // Todo IM 特性：如果集成了 WebSocket，在这里推送一条同步指令给当前用户的各个终端
                    // webSocketMessageService.sendSyncCommandToUser(userId, "CONTACT_DELETED", id);
                }
            });
        } else {
            // 如果由于某种原因没有事务，直接删缓存
            redisTemplateUtil.deleteContactCache(userId);
        }

        return true;
    }

    /**
     * 修改联系人
     *
     * @param updateContactDTO 修改联系人DTO
     * @param userId           用户id
     * @return boolean
     */
    @Transactional
    @Override
    public boolean update(UpdateContactDTO updateContactDTO, Long userId) {
        Contact contact = contactDao.selectById(updateContactDTO.getContactId());
        if (contact == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.CONTACT_NOT_EXIST_MSG);
        }
        if (!contact.getUserId().equals(userId)) {
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE, ExceptionConstant.NOT_ALLOWED_MSG);
        }
        if (updateContactDTO.getRemark() != null && !updateContactDTO.getRemark().isEmpty())
            contact.setRemark(updateContactDTO.getRemark());
        else
            contact.setRemark("");
        contactDao.updateById(contact);

        // 用户联系人列表缓存信息失效
        redisTemplateUtil.deleteContactCache(userId);

        return true;
    }

    /**
    * 获取用户信息
     *
     * @param userId 用户ID
     *
    * */
    public User getUserInfo(Long userId) {
        User user = new User();
        // 先从缓存中获取用户信息
        UserInfoCache userInfoCache = (UserInfoCache) redisTemplate.
                opsForValue().
                get(RedisConstant.USER_INFO + userId);

        if (userInfoCache != null) {
            user.setId(userId);
            user.setUsername(userInfoCache.getUsername());
            user.setNickname(userInfoCache.getNickname());
            user.setAvatar(userInfoCache.getAvatar());
            user.setGender(userInfoCache.getGender());
            user.setBio(userInfoCache.getBio());
        }
        // 用户缓存信息不存在，则从数据库中获取
        else {
            User userTmp = userDao.selectById(userId);
            user.setId(userId);
            user.setUsername(userTmp.getUsername());
            user.setNickname(userTmp.getNickname());
            user.setAvatar(userTmp.getAvatar());
            user.setGender(userTmp.getGender());
            user.setBio(userTmp.getBio());
            // 将用户信息写入缓存，并设置随机时间(30~40min)，防止缓存雪崩
            userInfoCache = UserInfoCache.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .gender(user.getGender())
                    .bio(user.getBio())
                    .build();
            redisTemplate.opsForValue().set(RedisConstant.USER_INFO + userId, userInfoCache,
                    RedisConstant.USER_INFO_DURATION + (long) (Math.random() * 10), TimeUnit.MINUTES
            );
        }
        return user;
    }
}
