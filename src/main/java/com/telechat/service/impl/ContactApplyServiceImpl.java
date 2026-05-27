/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午8:49
 */
package com.telechat.service.impl;

import com.telechat.annotation.FrequencyLock;
import com.telechat.cache.ContactApplyCacheService;
import com.telechat.cache.ContactCacheService;
import com.telechat.cache.ConversationCacheService;
import com.telechat.cache.UserInfoCacheService;
import com.telechat.constant.ExceptionConstant;
import com.telechat.exception.exceptions.ContactException;
import com.telechat.mapper.dao.ContactApplyDao;
import com.telechat.mapper.dao.ContactDao;
import com.telechat.mapper.dao.ConversationDao;
import com.telechat.mapper.dao.ConversationMemberDao;
import com.telechat.cache.entity.ContactApplyCache;
import com.telechat.cache.entity.UserInfoCache;
import com.telechat.mq.event.ContactConversationEvent;
import com.telechat.mq.publisher.ContactConversationEventPublisher;
import com.telechat.pojo.dto.contact.ContactApplyHandleDTO;
import com.telechat.pojo.enums.mq.ContactConversationType;
import com.telechat.pojo.vo.ContactApplyResultVO;
import com.telechat.pojo.vo.ConversationVO;
import com.telechat.util.AfterCommitUtil;
import com.telechat.pojo.entity.*;
import com.telechat.pojo.enums.ContactApplyStatus;
import com.telechat.pojo.enums.ConversationMemberRole;
import com.telechat.pojo.enums.ConversationType;
import com.telechat.pojo.vo.ContactApplyVO;
import com.telechat.service.ContactApplyService;
import com.telechat.service.UserService;
import com.telechat.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContactApplyServiceImpl implements ContactApplyService {
    // service
    @Resource
    private UserService userService;

    @Resource
    private UserInfoCacheService userInfoCacheService;

    @Resource
    private ContactCacheService contactCacheService;

    @Resource
    private ContactApplyCacheService contactApplyCacheService;

    @Resource
    private ConversationCacheService conversationCacheService;

    // dao
    @Resource
    private ContactDao contactDao;

    @Resource
    private ContactApplyDao contactApplyDao;

    @Resource
    private ConversationDao conversationDao;

    @Resource
    private ConversationMemberDao conversationMemberDao;

    // mq
    @Resource
    private ContactConversationEventPublisher contactConversationEventPublisher;

    // util
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @FrequencyLock(
            key = "'lock:contact:apply:' + #userId + ':' + #contactUserName",
            waitTime = 0,
            msg = "请勿重复提交申请"
    )
    public ContactApplyResultVO addContactApply(Long userId, String contactUserName) {
        // 1. 基础校验 (前置防御)
        Long contactId = userService.getUserIdByUsername(contactUserName);
        if (contactId == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.USER_NOT_EXIST_MSG);
        }
        if (userId.equals(contactId)) {
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE, "不能添加自己为好友");
        }

        UserInfoCache senderInfo = userInfoCacheService.getUserInfoCache(userId);
        if (senderInfo == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.USER_NOT_EXIST_MSG);
        }

        // 2. 校验是否已是联系人
        Contact contact = contactDao.selectByUserIdAndFriendId(userId, contactId);
        if (contact != null) {
            throw new ContactException(ExceptionConstant.ALREADY_EXIST_CODE, "你们已经是好友了");
        }

        LocalDateTime now = LocalDateTime.now();

        // ==========================================
        // 场景 A：检查对方是否已经向我发过申请且处于 PENDING 状态（双向奔赴/互粉逻辑）
        // ==========================================
        ContactApply reverseApply = contactApplyDao.selectByUserIdAndFriendId(contactId, userId);
        if (reverseApply != null && reverseApply.getStatus() == ContactApplyStatus.PENDING) {
            // 构造处理申请的DTO，模拟同意对方的请求
            ContactApplyHandleDTO handleDTO = new ContactApplyHandleDTO();
            handleDTO.setContactId(reverseApply.getId());
            handleDTO.setAgree(true);

            // 调用处理申请的方法，直接生成并获取会话
            ConversationVO conversation = handleApply(userId, handleDTO);

            // 返回状态1，并携带生成的会话信息
            return ContactApplyResultVO.builder()
                    .status(1)
                    .conversationVO(conversation)
                    .build();
        }

        // ==========================================
        // 场景 B：正常发送申请逻辑
        // ==========================================
        ContactApply existingApply = contactApplyDao.selectByUserIdAndFriendId(userId, contactId);

        if (existingApply == null) {
            ContactApply newApply = ContactApply.builder()
                    .id(snowflakeIdGenerator.nextId())
                    .userId(userId)
                    .friendId(contactId)
                    .status(ContactApplyStatus.PENDING)
                    .createdTime(now)
                    .isRead(false)
                    .build();
            contactApplyDao.insert(newApply);
        } else {

            // 防止防刷逻辑：如果已存在但不是 PENDING，则更新为 PENDING
            if (existingApply.getStatus() != ContactApplyStatus.PENDING) {
                existingApply.setStatus(ContactApplyStatus.PENDING);
                existingApply.setIsRead(false);
                existingApply.setCreatedTime(now); // 更新时间，提升列表排序
                contactApplyDao.updateById(existingApply);
            }
            // 如果请求已存在且未处理，那么就不允许重复发送
            else{
                throw new ContactException(ExceptionConstant.ALREADY_EXIST_CODE,ExceptionConstant.CONTACT_APPLY_ALREADY_EXIST_MSG);
            }
        }

        // ==========================================
        // 3. 注册事务提交后置钩子 (保证数据一致性)
        // ==========================================
        AfterCommitUtil.executeAfterCommit(() -> {
            try {
                // 构建好友申请消息
                ContactConversationEvent event = ContactConversationEvent.builder()
                        .contactConversationType(ContactConversationType.CONTACT_APPLY)
                        .senderId(userId)
                        .receiverId(contactId)
                        .description(senderInfo.getNickname()+" 请求添加你为好友")
                        .timestamp(System.currentTimeMillis())
                        .build();

                contactConversationEventPublisher.publishContactConversationEvent(event);
            } catch (Exception e) {
                log.error("好友申请后置操作(缓存/WS通知)异常, contactId: {}", contactId, e);
            }
        });

        // 返回状态0，表示仅发送申请
        return ContactApplyResultVO.builder()
                .status(0)
                .build();
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
        List<ContactApplyCache> contactApplyCacheList = contactApplyCacheService.getContactApplyCache(userId);


        // 2. 提取发起人的 ID 集合
        Set<Long> senderIds = contactApplyCacheList.stream()
                .map(ContactApplyCache::getUserId)
                .collect(Collectors.toSet());

        // 3. 批量查询用户信息(查到的用户信息自动计入缓存，缓解数据库压力)
        // Map<UserId, UserEntity>
        Map<Long, UserInfoCache> userMap = userInfoCacheService.getUserInfoCacheMapByIds(senderIds);

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
     * @return ConversationVO
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 内层事务 Lock(Transactional)
    @FrequencyLock(
            key = "'lock:contact:handle:' + #dto.contactId", // 支持解析对象属性
            msg = "该申请正在处理中"
    )
    public ConversationVO handleApply(Long userId, ContactApplyHandleDTO dto) {
        // dto.getContactId() applyId
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
            AfterCommitUtil.executeAfterCommit(() -> contactApplyCacheService.deleteContactApplyCache(userId));
            return null;
        }

        // 3. 同意逻辑
        LocalDateTime now = LocalDateTime.now();

        // 3.1 更新申请状态
        contactApply.setStatus(ContactApplyStatus.ACCEPTED);
        contactApplyDao.updateById(contactApply);

        // 3.2 创建私聊会话
        String privateKey = this.generatePrivateUniqueKey(contactApply.getFriendId(),contactApply.getUserId());
        Conversation oldConversation = conversationDao.selectByUniqueKey(privateKey);
        Conversation conversation;
        // 如果不存在旧会话，新建一个会话
        if (oldConversation == null) {
            conversation = Conversation.builder()
                    .id(snowflakeIdGenerator.nextId())
                    .type(ConversationType.PRIVATE)
                    .uniqueKey(privateKey)
                    .createdTime(now)
                    .updatedTime(now)
                    .lastMessageContent("我们已经添加好友，开始聊天吧!")
                    .lastMessageTime(now)
                    .build();
            conversationDao.insert(conversation);
        }
        // 存在，复用（需要更新状态）
        else{
            oldConversation.setCreatedTime(now);
            oldConversation.setUpdatedTime(now);
            oldConversation.setLastMessageTime(now);
            conversation = oldConversation;
            conversationDao.update(conversation);
        }

        // 3.3 建立双向好友关系 (A->B 和 B->A) 已防止数据重复
        // A = 申请发起人 (contactApply.getUserId())
        // B = 我 (contactApply.getFriendId())

        // 处理 A -> B
        upsertContact(contactApply.getUserId(),
                contactApply.getFriendId(),
                conversation.getId(),
                now);
        // 处理 B -> A
        upsertContact(contactApply.getFriendId(),
                contactApply.getUserId(),
                conversation.getId(),
                now);

        // 3.4 添加会话成员（A 和 B）已防止数据重复
        // A = 申请发起人 (contactApply.getUserId())
        // B = 我 (contactApply.getFriendId())

        // A
        upsertConversationMember(
              conversation.getId(),
              contactApply.getUserId(),
              now
        );

        // B
        upsertConversationMember(
                conversation.getId(),
                contactApply.getFriendId(),
                now
        );

        double score = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();


        // 将所有删除缓存的操作，注册到事务提交后的回调中执行
        Conversation finalConversation = conversation;

        // 用户自己的信息
        UserInfoCache userInfoCacheA = userInfoCacheService.getUserInfoCache(contactApply.getFriendId());

        // 好友的信息
        UserInfoCache userInfoCacheB = userInfoCacheService.getUserInfoCache(contactApply.getUserId());

        ConversationVO conversationVOForFriend = ConversationVO.builder()
                .id(finalConversation.getId())
                .type(ConversationType.PRIVATE)
                .title(userInfoCacheA.getNickname())
                .avatar(userInfoCacheA.getAvatar())
                .unreadCount(1)
                .isTop(false)
                .isMuted(false)
                .lastMessageContent("我们已经添加好友，开始聊天吧!")
                .lastMessageTime(now)
                .score(score)
                .build();

        ConversationVO conversationVOForMe = ConversationVO.builder()
                .id(finalConversation.getId())
                .type(ConversationType.PRIVATE)
                .title(userInfoCacheB.getNickname())
                .avatar(userInfoCacheB.getAvatar())
                .unreadCount(1)
                .isTop(false)
                .isMuted(false)
                .lastMessageContent("我们已经添加好友，开始聊天吧!")
                .lastMessageTime(now)
                .score(score)
                .build();


        AfterCommitUtil.executeAfterCommit(() -> {
            // 删除联系人列表缓存
            contactCacheService.deleteContactCache(
                    List.of(contactApply.getUserId(), contactApply.getFriendId())
            );

            // 删除好友申请列表缓存
            contactApplyCacheService.deleteContactApplyCache(userId);

            // 添加 conversationId 到 userId和 friendId 的 ZSet
            conversationCacheService.addConversationToZSetSafe(userId, finalConversation.getId(), score);
            conversationCacheService.addConversationToZSetSafe(contactApply.getUserId(), finalConversation.getId(), score);

            // 构建同意好友申请消息
            ContactConversationEvent event = ContactConversationEvent.builder()
                    .contactConversationType(ContactConversationType.APPLY_AGREE)
                    .senderId(userId)
                    .receiverId(contactApply.getUserId())
                    .conversationVO(conversationVOForFriend)
                    .description("同意添加你为好友")
                    .timestamp(System.currentTimeMillis())
                    .build();
            contactConversationEventPublisher.publishContactConversationEvent(event);
        });

        return conversationVOForMe;
    }

    public String generatePrivateUniqueKey(Long userId1, Long userId2) {
        // 逻辑：谁小谁在前
        long minId = Math.min(userId1, userId2);
        long maxId = Math.max(userId1, userId2);
        return "P:" + minId + "_" + maxId;
    }

    /**
     * 更新或者插入联系人信息
     * @param uId fId convId now
     */
    private void upsertContact(Long uId, Long fId, Long convId, LocalDateTime now) {
        Contact existing = contactDao.selectByUserIdAndFriendId(uId, fId);
        if (existing != null) {
            // 如果存在（之前单向删除留下的残留），更新关键字段即可
            existing.setConversationId(convId);
            existing.setCreatedTime(now); // 重新标记添加时间
            contactDao.updateById(existing);
        } else {
            // 不存在，直接插入
            Contact newContact = Contact.builder()
                    .id(snowflakeIdGenerator.nextId())
                    .userId(uId)
                    .friendId(fId)
                    .conversationId(convId)
                    .createdTime(now).build();
            contactDao.insert(newContact);
        }
    }

    /**
     * 更新或者插入会话成员信息
     * @param uId fId convId now
     */
    private void upsertConversationMember(Long convId, Long uId, LocalDateTime now) {
        ConversationMember existing = conversationMemberDao.selectByConversationIdAndUserId(convId, uId);
        if (existing != null) {
            // 如果存在（之前单向删除留下的残留），更新关键字段即可
            existing.setConversationId(convId);
            existing.setJoinedTime(now); // 重新标记添加时间
            conversationMemberDao.updateById(existing);
        } else {
            // 不存在，直接插入
            ConversationMember newConversationMember = ConversationMember.builder()
                    .id(snowflakeIdGenerator.nextId())
                    .conversationId(convId)
                    .userId(uId)
                    .role(ConversationMemberRole.MEMBER)
                    .joinedTime(now)
                    .build();
            conversationMemberDao.insert(newConversationMember);
        }
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
