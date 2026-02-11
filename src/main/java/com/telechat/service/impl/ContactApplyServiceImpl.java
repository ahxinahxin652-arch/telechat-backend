/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午8:49
 */
package com.telechat.service.impl;

import com.telechat.annotation.FrequencyLock;
import com.telechat.constant.ExceptionConstant;
import com.telechat.constant.RedisConstant;
import com.telechat.exception.exceptions.ContactException;
import com.telechat.mapper.ContactApplyMapper;
import com.telechat.mapper.dao.ContactApplyDao;
import com.telechat.mapper.dao.ContactDao;
import com.telechat.mapper.dao.ConversationDao;
import com.telechat.mapper.dao.ConversationMemberDao;
import com.telechat.pojo.cache.ContactApplyCache;
import com.telechat.pojo.cache.UserInfoCache;
import com.telechat.pojo.dto.contact.ContactApplyHandleDTO;
import com.telechat.pojo.dto.ws.ContactApplyNotification;
import com.telechat.pojo.entity.*;
import com.telechat.pojo.enums.ContactApplyStatus;
import com.telechat.pojo.enums.ConversationStatus;
import com.telechat.pojo.enums.ConversationType;
import com.telechat.pojo.vo.ContactApplyVO;
import com.telechat.service.ContactApplyService;
import com.telechat.service.UserService;
import com.telechat.util.RedisTemplateUtil;
import com.telechat.websocket.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContactApplyServiceImpl implements ContactApplyService {

    @Autowired
    private UserService userService;

    @Autowired
    private ContactDao contactDao;

    @Autowired
    private ContactApplyDao contactApplyDao;

    @Autowired
    private ConversationDao conversationDao;

    @Autowired
    private ConversationMemberDao conversationMemberDao;

    @Autowired
    private RedisTemplateUtil redisTemplateUtil;

    @Autowired
    private MessageService messageService;
    @Autowired
    private ContactApplyMapper contactApplyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @FrequencyLock(
            key = "'lock:contact:apply:' + #userId + ':' + #contactUserName",
            waitTime = 0,
            msg = "请勿重复提交申请"
    )
    public boolean addContactApply(Long userId, String contactUserName) {
        // 1. 基础校验
        Long contactId = userService.getUserIdByUsername(contactUserName);
        if (contactId == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.USER_NOT_EXIST_MSG);
        }
        if (userId.equals(contactId)) {
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE, ExceptionConstant.NOT_ALLOWED_SEND_APPLY_MYSELF);
        }

        UserInfoCache senderInfo = redisTemplateUtil.getUserInfoCache(userId);
        if (senderInfo == null) {
            // 如果这里查不到，说明数据异常
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.USER_NOT_EXIST_MSG);
        }

        // 2. 校验是否已添加联系人
        Contact contact = contactDao.selectByUserIdAndFriendId(userId, contactId);
        if (contact != null) {
            throw new ContactException(ExceptionConstant.ALREADY_EXIST_CODE, ExceptionConstant.CONTACT_ALREADY_EXIST_MSG);
        }

        // --- 2. 写操作 (开启事务) ---
        return executeAddApplyTransaction(userId, contactId, senderInfo);
    }

    /**
     * 将事务逻辑抽取为独立方法，确保事务粒度最小化
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean executeAddApplyTransaction(Long userId, Long contactId, UserInfoCache senderInfo) {
        LocalDateTime now = LocalDateTime.now();

        // 场景 A：检查对方是否已经向我发过申请且处于 PENDING 状态（互粉逻辑）
        ContactApply reverseApply = contactApplyDao.selectByUserIdAndFriendId(contactId, userId);
        if (reverseApply != null && reverseApply.getStatus() == ContactApplyStatus.PENDING) {
            // 直接复用处理申请的逻辑，传入“同意”
            ContactApplyHandleDTO handleDTO = new ContactApplyHandleDTO();
            handleDTO.setContactId(reverseApply.getId()); // 注意这里是 applyId
            handleDTO.setAgree(true);
            return handleApply(userId, handleDTO);
        }

        // 场景 B：正常申请逻辑
        ContactApply existingApply = contactApplyDao.selectByUserIdAndFriendId(userId, contactId);
        Long finalApplyId;

        if (existingApply == null) {
            ContactApply newApply = ContactApply.builder()
                    .userId(userId).friendId(contactId)
                    .status(ContactApplyStatus.PENDING)
                    .createdTime(now)
                    .isRead(false)
                    .build();
            contactApplyDao.insert(newApply);
            finalApplyId = newApply.getId();
        } else {
            // 如果已存在记录，仅在非 PENDING 状态下更新，防止重复刷
            if (existingApply.getStatus() != ContactApplyStatus.PENDING) {
                existingApply.setStatus(ContactApplyStatus.PENDING);
                existingApply.setIsRead(false);
                existingApply.setCreatedTime(now); // 更新时间以便排在最前
                contactApplyDao.updateById(existingApply);
            }
            finalApplyId = existingApply.getId();
        }

        // 清除接收者的申请列表缓存
        redisTemplateUtil.deleteContactApplyCache(contactId);

        // 注册事务回调发送 WebSocket
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    ContactApplyNotification notification = ContactApplyNotification.builder()
                            .applyId(finalApplyId)
                            .senderId(userId)
                            .nickname(senderInfo.getNickname())
                            .avatar(senderInfo.getAvatar())
                            .description("请求添加你为好友")
                            .createTime(now)
                            .build();
                    messageService.sendContactApplyNotification(contactId, notification);
                } catch (Exception e) {
                    log.error("好友申请通知推送失败", e);
                }
            }
        });
        return true;
    }

    /**
     * 获取联系人申请列表【未处理】
     * 优化：解决了 N+1 查询问题
     */
    @Override // todo 分页获取请求，redis如何进行分页数据的缓存和获取？
    public List<ContactApplyVO> applyList(Long userId) {
        // 查询所有发给我的申请 (注意：这里 userId 应该是 receiver/friendId)
        // 假设 selectApplyList 的 SQL 逻辑是 WHERE friend_id = #{userId} AND status = #{status}
        // 使用枚举对象传参，MybatisPlus 会自动处理

        // 1. 从缓存中读取（方法已内置缓存写入，并解决了缓存穿透等问题）
        List<ContactApplyCache> contactApplyCacheList = redisTemplateUtil.getContactApplyCache(userId);


        // 2. 提取发起人的 ID 集合
        Set<Long> senderIds = contactApplyCacheList.stream()
                .map(ContactApplyCache::getUserId)
                .collect(Collectors.toSet());

        // 3. 批量查询用户信息(查到的用户信息自动计入缓存，缓解数据库压力)
        // Map<UserId, UserEntity>
        Map<Long, UserInfoCache> userMap = redisTemplateUtil.getUserInfoCacheMapByIds(senderIds);

        // 4. 组装 VO
        return contactApplyCacheList.stream().map(apply -> {
            UserInfoCache sender = userMap.get(apply.getUserId());
            return ContactApplyVO.builder()
                    .id(apply.getContactApplyId())
                    .userId(apply.getUserId())
                    .avatar(sender != null ? sender.getAvatar() : null)
                    .nickname(sender != null ? sender.getNickname() : "未知用户")
                    .status(apply.getStatus())
                    .createTime(apply.getCreatedTime())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 处理联系人申请
     *
     * @param userId                用户ID
     * @param dto 处理联系人申请DTO
     * @return boolean
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 内层事务 Lock(Transactional)
    @FrequencyLock(
            key = "'lock:contact:handle:' + #dto.contactId", // 支持解析对象属性
            msg = "该申请正在处理中"
    )
    // todo 插入可以性能优化
    public boolean handleApply(Long userId, ContactApplyHandleDTO dto) {
        // dto.getContactId() 名字可能有歧义，实际应该是 applyId
        Long applyId = dto.getContactId();

        ContactApply contactApply = contactApplyDao.selectById(applyId);

        // 1. 严格校验
        if (contactApply == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.CONTACT_NOT_EXIST_MSG);
        }

        // 安全校验：确认当前操作人是这个申请的接收方
        if (!contactApply.getFriendId().equals(userId)) {
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE, "无权处理他人的好友申请");
        }

        if (contactApply.getStatus() != ContactApplyStatus.PENDING) {
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE, ExceptionConstant.CONTACT_ALREADY_HANDLE_EXCEPTION_MSG);
        }

        // 2. 拒绝逻辑
        if (!dto.isAgree()) {
            contactApply.setStatus(ContactApplyStatus.REJECTED);
            contactApplyDao.updateById(contactApply);
            // 联系人申请缓存失效
            redisTemplateUtil.deleteContactApplyCache(userId);
            return true;
        }

        // 3. 同意逻辑
        // 双方建立了关系，缓存失效（联系人及联系申请缓存）
        redisTemplateUtil.deleteContactCache(
                List.of(contactApply.getUserId(), contactApply.getFriendId())
        );
        redisTemplateUtil.deleteContactApplyCache(userId);
        LocalDateTime now = LocalDateTime.now();

        // 3.1 更新申请状态
        contactApply.setStatus(ContactApplyStatus.ACCEPTED);
        contactApplyDao.updateById(contactApply);

        // 3.2 创建私聊会话
        // TODO: 最好检查一下是否之前存在过旧的私聊会话，如果是，可以复用
        Conversation conversation = Conversation.builder()
                .type(ConversationType.PRIVATE)
                .status(ConversationStatus.NORMAL)
                .createdTime(now)
                .updatedTime(now)
                .build();
        conversationDao.insert(conversation);

        // 3.3 建立双向好友关系 (A->B 和 B->A)
        // A = 申请发起人 (contactApply.getUserId())
        // B = 我 (contactApply.getFriendId())

        // 记录 1: A 的好友列表里有 B
        Contact contactForSender = Contact.builder()
                .userId(contactApply.getUserId())      // A
                .friendId(contactApply.getFriendId())  // B
                .conversationId(conversation.getId())
                .createdTime(now)
                .build();

        // 记录 2: B 的好友列表里有 A
        Contact contactForReceiver = Contact.builder()
                .userId(contactApply.getFriendId())    // B
                .friendId(contactApply.getUserId())    // A
                .conversationId(conversation.getId())
                .createdTime(now)
                .build();

        contactDao.insert(contactForSender);
        contactDao.insert(contactForReceiver);

        // 3.4 添加会话成员
        ConversationMember memberA = ConversationMember.builder()
                .conversationId(conversation.getId())
                .userId(contactApply.getUserId())
                .joinedTime(now)
                .build(); // 其他字段用默认值即可

        ConversationMember memberB = ConversationMember.builder()
                .conversationId(conversation.getId())
                .userId(contactApply.getFriendId())
                .joinedTime(now)
                .build();

        conversationMemberDao.insert(memberA);
        conversationMemberDao.insert(memberB);

        return true;
    }

    /**
     * 查询未读好友请求数量
     *
     * @param userId                用户ID
     * @return Long
     */
    public Long getUnreadCount(Long userId) {
        // 直接查 DB (有了索引速度很快)
        return contactApplyDao.getUnreadContactApplyCount(userId);
    }

    /**
     * 标记所有好友请求已读
     *
     * @param userId                用户ID
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        contactApplyDao.markAll(userId);
    }
}
