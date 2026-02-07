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
        // 1. 基础校验 (保持不变)
        Long contactId = userService.getUserIdByUsername(contactUserName);
        if (contactId == null) {
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.USER_NOT_EXIST_MSG);
        }
        if (userId.equals(contactId)) {
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE, ExceptionConstant.NOT_ALLOWED_SEND_APPLY_MYSELF);
        }

        // 2. 校验是否已添加联系人
        Contact contact = contactDao.selectByUserIdAndFriendId(userId, contactId);
        if (contact != null) {
            throw new ContactException(ExceptionConstant.ALREADY_EXIST_CODE, ExceptionConstant.CONTACT_ALREADY_EXIST_MSG);
        }

        // ---------------------------------------------------------
        // 【性能优化】提前准备好通知所需的数据 (Sender Info)
        // 优先查 Redis，避免在事务锁内进行不必要的 DB 查询
        // ---------------------------------------------------------
        UserInfoCache senderInfo = redisTemplateUtil.getUserInfoCache(userId);
        if (senderInfo == null) {
            // 如果这里查不到，说明数据异常
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.USER_NOT_EXIST_MSG);
        }

        // 3. 核心逻辑：Upsert (更新或插入)
        ContactApply existingApply = contactApplyDao.selectByUserIdAndFriendId(userId, contactId);

        // 定义一个 final 变量用于后续 WebSocket 通知，解决作用域问题
        Long finalApplyId;
        LocalDateTime now = LocalDateTime.now();

        if (existingApply == null) {
            // --- 插入新记录 ---
            ContactApply newApply = ContactApply.builder()
                    .userId(userId)
                    .friendId(contactId)
                    .status(ContactApplyStatus.PENDING)
                    .createdTime(now) // 显式设置时间
                    .build();
            contactApplyDao.insert(newApply);

            // 获取自增ID
            finalApplyId = newApply.getId();
        } else {
            // --- 复活旧记录 ---
            // 只有非 PENDING 状态才更新，避免重复刷新造成骚扰
            if (existingApply.getStatus() != ContactApplyStatus.PENDING) {
                existingApply.setStatus(ContactApplyStatus.PENDING);
                existingApply.setIsRead(false);    // 重新设为未读
                existingApply.setCreatedTime(now); // 更新时间顶到最前
                contactApplyDao.updateById(existingApply);
            }
            finalApplyId = existingApply.getId();
        }

        // 删除联系人申请缓存 (确保列表查询是最新的)
        redisTemplateUtil.deleteContactApplyCache(contactId);

        // ==========================================
        // 核心修改：发送 WebSocket 通知 (安全集成)
        // ==========================================

        // 构建通知对象 (必须使用 effectively final 的变量)
        ContactApplyNotification notification = ContactApplyNotification.builder()
                .applyId(finalApplyId)
                .senderId(userId)
                .nickname(senderInfo.getNickname())
                .avatar(senderInfo.getAvatar())
                .description("请求添加你为好友") // 可扩展为参数传入
                .createTime(now)
                .build();

        // 【关键点】注册事务同步回调
        // 机制：Spring 会把这个 Runnable 挂起，直到前面的数据库事务 commit 成功后才执行
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 此时数据库事务已提交，B 用户收到通知后立即调 API 也能查到数据
                    messageService.sendContactApplyNotification(contactId, notification);
                } catch (Exception e) {
                    // WebSocket 推送失败不应回滚业务，仅记录日志
                    // 毕竟申请已经入库了，用户刷新列表也能看到
                    log.error("好友申请 WebSocket 推送失败: receiverId={}", contactId, e);
                }
            }
        });

        return true;
    }

    /**
     * 获取联系人申请列表【未处理】
     * 优化：解决了 N+1 查询问题
     */
    @Override // todo 分页获取请求
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
