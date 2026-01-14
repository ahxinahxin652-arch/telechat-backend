/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午11:26
 */
package com.telechat.service.impl;

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
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO 添加Redis缓存，并注意解决缓存穿透、缓存雪崩、缓存击穿等问题
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

    /**
     * 获取联系人列表
     *
     * @param userId 用户ID
     * @return List<ContactVO>
     */
    @Override
    public List<ContactVO> list(Long userId) {
        // 1. 从Redis中获取该用户的联系人缓存信息
        List<ContactsCache> contactsCachesFromRedis = (List<ContactsCache>) redisTemplate.opsForValue().get(RedisConstant.USER_CONTACTS_INFO + userId);

        // 2. 获取缓存信息不为null且不为""
        if(contactsCachesFromRedis != null){
            if(!contactsCachesFromRedis.isEmpty()){
                // 缓存命中，将缓存信息转换为ContactVO
                List<ContactVO> contactVOS = new ArrayList<>();
                // TODO 联系人列表的用户信息批量加载
                contactsCachesFromRedis.forEach(contactsCache -> {
                    ContactVO contactVO = ContactVO.builder()
                            .id(contactsCache.getContactId())
                            .userId(contactsCache.getFriendId())
                            .remark(contactsCache.getRemark())
                            .build();

                    // 获取用户信息(函数已包含Redis缓存，数据库逻辑)
                    User user = this.getUserInfo(contactsCache.getFriendId());
                    contactVO.setUsername(user.getUsername());
                    contactVO.setNickname(user.getNickname());
                    contactVO.setAvatar(user.getAvatar());
                    contactVOS.add(contactVO);
                });
                return contactVOS;
            }
            // 缓存命中空列表，返回空列表
            else{
                return new ArrayList<>();
            }
        }

        // 3. 缓存未命中，从数据库中查询
        List<Contact> contacts = contactDao.list(userId);

        // 4. 如果联系人列表为空，redis存入Collections.emptyList() 30分钟（防止缓存穿透）
        if (contacts == null || contacts.isEmpty()) {
            redisTemplate.opsForValue().
                    set(RedisConstant.USER_CONTACTS_INFO + userId, Collections.emptyList(), RedisConstant.USER_CONTACTS_INFO_DURATION, TimeUnit.MINUTES);
            return new ArrayList<>();
        }
        // 5. 初始化 List<ContactVO>，List<ContactsCache>
        List<ContactVO> contactVOS = new ArrayList<>();
        List<ContactsCache> contactsCaches = new ArrayList<>();
        // TODO 联系人列表的用户信息批量加载
        contacts.forEach(contact -> {
            ContactVO contactVO = ContactVO.builder()
                    .id(contact.getId())
                    .userId(contact.getFriendId())
                    .remark(contact.getRemark())
                    .build();

            ContactsCache contactsCache = ContactsCache.builder()
                    .contactId(contact.getId())
                    .friendId(contact.getFriendId())
                    .remark(contact.getRemark())
                    .build();

            // 获取用户信息(函数已包含Redis缓存，数据库逻辑)
            User user = this.getUserInfo(contact.getFriendId());
            contactVO.setUsername(user.getUsername());
            contactVO.setNickname(user.getNickname());
            contactVO.setAvatar(user.getAvatar());

            contactVOS.add(contactVO);
            contactsCaches.add(contactsCache);
        });
        // 6. 缓存联系人列表信息
        redisTemplate.opsForValue().set(RedisConstant.USER_CONTACTS_INFO + userId, contactsCaches, RedisConstant.USER_CONTACTS_INFO_DURATION, TimeUnit.MINUTES);
        return contactVOS;
    }

    /**
     * 删除联系人
     *
     * @param id     联系人ID
     * @param userId 用户ID
     * @return boolean
     */
    @Transactional
    @Override
    public boolean delete(Long id, Long userId) {
        // 查找联系人是否存在
        Contact contact = contactDao.selectById(id);
        if (contact == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.CONTACT_NOT_EXIST_MSG);
        }
        if (!contact.getUserId().equals(userId)) {
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE, ExceptionConstant.NOT_ALLOWED_MSG);
        }

        // 删除联系人
        contactDao.delete(contact.getId());

        // 设置会话删除状态
        Long conversationId = contact.getConversationId();
        if (conversationId != null) {
            ConversationMember conversationMember = conversationMemberDao.selectByConversationIdAndUserId(conversationId, userId);
            conversationMember.setDeleted(true);
            conversationMemberDao.updateById(conversationMember);
        } else {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.CONVERSATION_NOT_EXIST_MSG);
        }

        // 用户联系人列表缓存信息失效
        redisTemplate.delete(RedisConstant.USER_CONTACTS_INFO + userId);

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
        redisTemplate.delete(RedisConstant.USER_CONTACTS_INFO + userId);

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
