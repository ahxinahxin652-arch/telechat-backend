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
    import com.telechat.mapper.dao.ContactApplyDao;
    import com.telechat.mapper.dao.ContactDao;
    import com.telechat.mapper.dao.ConversationDao;
    import com.telechat.mapper.dao.ConversationMemberDao;
    import com.telechat.pojo.cache.ContactApplyCache;
    import com.telechat.pojo.cache.UserInfoCache;
    import com.telechat.pojo.dto.contact.ContactApplyHandleDTO;
    import com.telechat.pojo.entity.*;
    import com.telechat.pojo.enums.ContactApplyStatus;
    import com.telechat.pojo.enums.ConversationStatus;
    import com.telechat.pojo.enums.ConversationType;
    import com.telechat.pojo.vo.ContactApplyVO;
    import com.telechat.service.ContactApplyService;
    import com.telechat.service.UserService;
    import com.telechat.util.RedisTemplateUtil;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Map;
    import java.util.Set;
    import java.util.UUID;
    import java.util.concurrent.TimeUnit;
    import java.util.stream.Collectors;

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

        /**
         * 添加联系申请
         *
         * @param userId    用户ID
         * @param contactUserName 用户名
         */
        @Override
        @Transactional(rollbackFor = Exception.class) // 内层事务 Lock(Transactional)
        @FrequencyLock(
                key = "'lock:contact:apply:' + #userId + ':' + #contactUserName",
                waitTime = 0, // 0表示不等待，立即失败(防止手抖重复点击)
                msg = "请勿重复提交申请"
        )
        public boolean addContactApply(Long userId, String contactUserName) {
            // 1. 基础校验
            Long contactId = userService.getUserIdByUsername(contactUserName);
            if(contactId == null){
                throw new ContactException(ExceptionConstant.NOT_EXIST_CODE,ExceptionConstant.USER_NOT_EXIST_MSG);
            }

            // 防止给自己发请求
            if (userId.equals(contactId)) {
                throw new ContactException(
                        ExceptionConstant.NOT_ALLOWED_CODE,
                        ExceptionConstant.NOT_ALLOWED_SEND_APPLY_MYSELF
                );
            }

            // 2. 校验是否已添加联系人 //todo 可以从redis中获取
            Contact contact = contactDao.selectByUserIdAndFriendId(userId, contactId);
            if(contact != null){
                throw new ContactException(ExceptionConstant.ALREADY_EXIST_CODE,ExceptionConstant.CONTACT_ALREADY_EXIST_MSG);
            }

            // 3. 核心优化：查询旧申请记录（若存在之前已被同意或拒绝的请求，重新复用）
            ContactApply existingApply = contactApplyDao.selectByUserIdAndFriendId(userId, contactId);

            if (existingApply == null) {
                // 不存在 -> 插入新记录
                ContactApply newApply = ContactApply.builder()
                        .userId(userId).friendId(contactId).status(ContactApplyStatus.PENDING)
                        .build();
                contactApplyDao.insert(newApply);
            } else {
                // 已存在 -> 更新旧记录 (复活)
                // 只有当状态不是 PENDING 时才更新，防止重复刷新时间
                if (existingApply.getStatus() != ContactApplyStatus.PENDING) {
                    existingApply.setStatus(ContactApplyStatus.PENDING);
                    existingApply.setCreatedTime(LocalDateTime.now()); // 更新时间，让它排到列表最前
                    contactApplyDao.updateById(existingApply);
                }
            }
            // 删除联系人申请缓存
            redisTemplateUtil.deleteContactApplyCache(contactId);

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
    }
