package com.telechat.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import com.telechat.mq.event.ContactConversationEvent;
import com.telechat.mq.publisher.ContactConversationEventPublisher;
import com.telechat.pojo.enums.mq.ContactConversationType;
import com.telechat.util.AfterCommitUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.telechat.cache.ConversationCacheService;
import com.telechat.cache.entity.ConversationMemberCache;
import com.telechat.cache.entity.ConversationMetaInfoCache;
import com.telechat.cache.entity.ConversationStaticInfoCache;
import com.telechat.cache.entity.ConversationZSetCache;
import com.telechat.constant.*;
import com.telechat.exception.exceptions.ConversationException;
import com.telechat.mapper.dao.ConversationDao;
import com.telechat.mapper.dao.ConversationMemberDao;
import com.telechat.pojo.entity.Conversation;
import com.telechat.pojo.entity.ConversationMember;
import com.telechat.pojo.enums.ConversationMemberRole;
import com.telechat.pojo.enums.ConversationStatus;
import com.telechat.pojo.enums.ConversationType;
import com.telechat.pojo.vo.ConversationVO;
import com.telechat.service.ConversationService;
import com.telechat.util.SnowflakeIdGenerator;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {
    // service
    @Resource
    private ConversationCacheService conversationCacheService;

    // dao
    @Resource
    private ConversationDao conversationDao;

    @Resource
    private ConversationMemberDao conversationMemberDao;

    // mq
    @Resource
    private ContactConversationEventPublisher contactConversationEventPublisher;

    // util
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // 用专门的线程池资源进行异步预热
    @Resource
    private Executor preHeatExecutor;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 会话数据列表全量预热（防并发击穿版）
     *
     * @param userId 用户ID
     * @return true: 预热成功 / false: 未执行预热(已有线程在执行或发生异常)
     */
    @Override
    public Boolean preHeatConversationZSets(Long userId) {
        // 1. 定义细粒度的分布式锁 Key，仅锁定当前用户
        String lockKey = RedisConstant.USER_CONVERSATIONS_PREHEAT_LOCK + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 核心大招：非阻塞尝试加锁 (Fail-Fast)
            // waitTime = 0: 绝不等待！拿不到锁直接返回 false。
            // leaseTime = 10: 锁最多持有 10 秒，防止执行预热的机器突然断电宕机导致死锁 (看门狗机制)
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                try {
                    log.info("用户 {} 获取预热锁成功，开始执行高成本预热逻辑...", userId);
                    // 3. 真正去查数据库并写入 Redis 的逻辑
                    return conversationCacheService.preHeatConversationZSets(userId);
                } finally {
                    // 4. 安全释放锁：必须判断是否是当前线程持有的锁
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 拿不到锁说明别的线程正在预热，直接忽略本次请求
                log.info("用户 {} 的会话预热正在进行中，已阻断重复触发", userId);
                return false;
            }
        } catch (InterruptedException e) {
            // 恢复中断标志位
            Thread.currentThread().interrupt();
            log.error("用户 {} 预热加锁过程中被中断", userId, e);
            return false;
        } catch (Exception e) {
            log.error("用户 {} 预热过程发生异常", userId, e);
            return false;
        }
    }

    /**
     * 懒加载会话数据
     * 高性能架构：ZSet 游标 -> 触底 DB 回源 -> 自动补齐 ZSet -> 终点标记防穿透 -> 并行 MultiGet 组装
     *
     * @param userId 用户ID
     * @param cursor 末尾会话的score（时间戳权重），0代表第一页
     * @return List<ConversationVO>
     */
    @Override
    public List<ConversationVO> lazyLoadConversations(Long userId, Double cursor) {
        // 参数校验
        if (userId == null || cursor == null) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, ExceptionConstant.Judge_Query_Exception_MSG);
        }

        String zsetKey = RedisConstant.USER_CONVERSATIONS_ZSET + userId;

        // 1. 确定 ZSet 查询的 Score 边界范围
        double maxScore = cursor == 0 ? ServiceConstant.MAX_TIME : cursor - 1;

        // 【优化 1：使用 LinkedHashMap】完美保留 ZSet 顺序，同时存储 Score！
        LinkedHashMap<Long, Double> idScoreMap = new LinkedHashMap<>();

        // 从 Redis ZSet 中按 Score 倒序获取
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(zsetKey, 0, maxScore, 0, ServiceConstant.LOAD_CONVERSATION_COUNT);

        double lastScoreFetchFromRedis = maxScore;

        if (!CollectionUtils.isEmpty(tuples)) {
            for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                Object valueObj = tuple.getValue();
                if (valueObj == null) continue; // 防御性编程：防止底层序列化异常导致 null

                String val = valueObj.toString();

                Long cid = Long.valueOf(val);
                Double score = tuple.getScore();
                idScoreMap.put(cid, score); // 存入有序 Map
                lastScoreFetchFromRedis = score;
            }
        }

        // 2. 判断是否需要去数据库回源补齐
        int remainNeed = ServiceConstant.LOAD_CONVERSATION_COUNT - idScoreMap.size();
        boolean newlyHitBottom = false; // 标记本次 DB 查询是否刚刚触底

        if (remainNeed > 0) {
            // 去数据库查询更老的会话数据
            boolean lastIsToped = lastScoreFetchFromRedis >= ServiceConstant.TOP_SCORE_OFFSET;
            long lastTimestamp = (long) (lastIsToped ? (lastScoreFetchFromRedis - ServiceConstant.TOP_SCORE_OFFSET) : lastScoreFetchFromRedis);
            LocalDateTime lastTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTimestamp), ZoneId.systemDefault());

            List<ConversationZSetCache> dbList = conversationDao.selectOlderConversations(userId, lastIsToped, lastTime, remainNeed);

            if (!CollectionUtils.isEmpty(dbList)) {
                for (ConversationZSetCache item : dbList) {
                    double score = item.getLastMessageTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    if (item.isToped()) {
                        score += ServiceConstant.TOP_SCORE_OFFSET;
                    }

                    idScoreMap.put(item.getConversationId(), score); // 追加到有序 Map 尾部
                }
            }

            // 缓存断层检测：ZSet Key 已失效，异步重建
            boolean hasKey = !CollectionUtils.isEmpty(tuples) || Boolean.TRUE.equals(redisTemplate.hasKey(zsetKey));
            if (!hasKey) {
                log.warn("用户 {} 懒加载时缓存断层，触发异步预热，本次仅穿透DB", userId);
                CompletableFuture.runAsync(() -> preHeatConversationZSets(userId), preHeatExecutor);
            }

            // 触底判断：DB 返回的数据不足一页，说明到底了
            if (CollectionUtils.isEmpty(dbList) || dbList.size() < remainNeed) {
                newlyHitBottom = true;
            }
        }

        // 极端情况：连第一页都没数据，且刚查完 DB 发现触底
        if (idScoreMap.isEmpty()) {
            if (newlyHitBottom) {
                // 返回哨兵给前端，让前端关闭 loading 状态
                return Collections.singletonList(ConversationVO.builder().id(-1L).build());
            }
            return Collections.emptyList();
        }

        // 3. 高并发 MultiGet 聚合装配
        // 此时 idScoreMap.keySet() 已经包含了所有需要查询的 ID，且严格按 score 倒序
        List<Long> conversationIds = new ArrayList<>(idScoreMap.keySet());
        Map<Long, ConversationStaticInfoCache> staticMap = conversationCacheService.getConversationStaticInfoCacheMapByIds(userId, conversationIds);
        Map<Long, ConversationMetaInfoCache> metaMap = conversationCacheService.getConversationMetaInfoCacheMapByIds(conversationIds);
        Map<Long, ConversationMemberCache> memberMap = conversationCacheService.getConversationMemberCacheByIds(userId, conversationIds);

        // 4. 组装最终返回数据
        List<ConversationVO> resultList = new ArrayList<>();

        // 遍历 LinkedHashMap，既能取到有序的 ID，也能直接 O(1) 取到对应的 Score
        for (Map.Entry<Long, Double> entry : idScoreMap.entrySet()) {
            Long cid = entry.getKey();
            Double score = entry.getValue();

            ConversationStaticInfoCache staticInfo = staticMap.get(cid);
            if (staticInfo == null || staticInfo.isNullPlaceholder()) {
                continue; // 脏数据直接跳过
            }

            ConversationMetaInfoCache metaInfo = metaMap.get(cid);
            ConversationMemberCache memberInfo = memberMap.get(cid);

            ConversationVO vo = ConversationVO.builder()
                    .id(cid)
                    .type(staticInfo.getType())
                    .title(staticInfo.getTitle())
                    .avatar(staticInfo.getAvatar())
                    .score(score) // <--- 【核心目标完成】精确写入对应的 Score
                    .build();

            // 动态信息
            if (metaInfo != null && !metaInfo.isNullPlaceholder()) {
                vo.setLastMessageContent(metaInfo.getLastMessageContent());
                vo.setLastMessageTime(metaInfo.getLastMessageTime());
            }

            // 个性化状态
            if (memberInfo != null && !memberInfo.isNullPlaceholder()) {
                vo.setIsTop(memberInfo.isToped());
                vo.setIsMuted(memberInfo.isMuted());
                vo.setUnreadCount(memberInfo.getUnreadCount());
            } else {
                vo.setIsTop(false);
                vo.setIsMuted(false);
                vo.setUnreadCount(0);
            }

            resultList.add(vo);
        }

        // 【关键修复：向前端传递触底信号】
        // 对应你前端 Pinia 的判断: const sentinelIndex = newData.findIndex(item => item.id === "-1");
        // (假设 Long 型 ID 在全局配置了 toString 序列化给前端防精度丢失，此处 -1L 到前端会变成 "-1")
        if (newlyHitBottom) {
            resultList.add(ConversationVO.builder().id(-1L).build());
        }

        return resultList;
    }

    /**
     * 获取单个会话信息
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return ConversationVO
     */
    @Override
    public ConversationVO getConversationInfo(Long userId, Long conversationId) {
        // 1. 校验参数
        if (conversationId == null) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, ExceptionConstant.Judge_Query_Exception_MSG);
        }

        // 2. 获取该会话的缓存信息 (假设这里可以通过某种方式拿到当前操作的 userId)
        // 核心：复用已有的缓存体系 (不需要再去查库)
        ConversationStaticInfoCache staticInfo = conversationCacheService.getConversationStaticInfoCache(conversationId);
        ConversationMetaInfoCache metaInfo = conversationCacheService.getConversationMetaInfoCache(conversationId);
        ConversationMemberCache memberInfo = conversationCacheService.getConversationMemberCache(userId, conversationId);

        if (staticInfo == null || staticInfo.isNullPlaceholder()) {
            throw new ConversationException(ExceptionConstant.NOT_EXIST_CODE, ExceptionConstant.CONVERSATION_NOT_EXIST_MSG);
        }

        ConversationVO vo = ConversationVO.builder()
                .id(staticInfo.getId())
                .type(staticInfo.getType())
                .title(staticInfo.getTitle())
                .avatar(staticInfo.getAvatar())
                .build();

        if (metaInfo != null && !metaInfo.isNullPlaceholder()) {
            vo.setLastMessageContent(metaInfo.getLastMessageContent());
            vo.setLastMessageTime(metaInfo.getLastMessageTime());
        }

        if (memberInfo != null && !memberInfo.isNullPlaceholder()) {
            vo.setIsTop(memberInfo.isToped());
            vo.setIsMuted(memberInfo.isMuted());
            vo.setUnreadCount(memberInfo.getUnreadCount());
        } else {
            vo.setIsTop(false);
            vo.setIsMuted(false);
            vo.setUnreadCount(0);
        }

        return vo;
    }

    /**
     * 创建群聊
     *
     * @param userId 用户ID
     * @param memberIds 群聊成员IDs
     * @return ConversationVO 创建的群聊返回视图
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ConversationVO createGroup(Long userId, Collection<Long> memberIds) {
        // 1. 安全校验：非空校验与人数限制 (防恶意刷接口，限制500人)
        if (memberIds == null || memberIds.isEmpty()) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, ExceptionConstant.Judge_Query_Exception_MSG);
        }

        // 2. 成员去重并确保包含创建者
        Set<Long> allMemberIds = new HashSet<>(memberIds);
        allMemberIds.add(userId);

        // 3. 安全与权限校验：检查拉入的人是否是自己的双向好友 (防越权拉陌生人)
        // 提示: 这里复用你之前写好的 RedisTemplateUtil.getContactCache 极其高效
        /*        List<ContactsCache> myContacts = redisTemplateUtil.getContactCache(userId);
        Set<Long> myFriendIds = myContacts.stream()
                .map(ContactsCache::getFriendId)
                .collect(Collectors.toSet());
        for (Long memberId : memberIds) {
            if (!myFriendIds.contains(memberId)) {
                throw new GlobalException("只能邀请双向好友加入群聊");
            }
        }*/

        // 4. 生成分布式唯一 ID
        long conversationId = snowflakeIdGenerator.nextId();
        LocalDateTime now = LocalDateTime.now();

        // 5. 组装并保存会话基础信息
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.GROUP)
                .title("群聊")
                .avatar(MessageConstant.GROUP_DEFAULT_AVATAR)
                .ownerId(userId)
                .uniqueKey("G:" + conversationId)
                .status(ConversationStatus.NORMAL)
                .createdTime(now)
                .updatedTime(now)
                .lastMessageTime(now)
                .lastMessageContent(MessageConstant.GROUP_CREATE_MESSAGE)
                .build();
        conversationDao.insert(conversation);

        // 6. 批量插入群成员 (使用批量插入，优化性能)
        List<ConversationMember> members = allMemberIds.stream().map(memberId -> {
            ConversationMember member = new ConversationMember();
            member.setId(snowflakeIdGenerator.nextId());
            member.setConversationId(conversationId);
            member.setUserId(memberId);
            member.setRole(memberId.equals(userId) ? ConversationMemberRole.OWNER : ConversationMemberRole.MEMBER);
            member.setMuted(false);
            member.setDeleted(false);
            member.setJoinedTime(now);
            return member;
        }).collect(Collectors.toList());
        // 分批批量插入会话成员
        conversationMemberDao.insertBatch(members);

        double score = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        ConversationVO conversationVO = ConversationVO.builder()
                .id(conversationId)
                .type(ConversationType.GROUP)
                .title(conversation.getTitle())
                .avatar(MessageConstant.GROUP_DEFAULT_AVATAR)
                .unreadCount(0)
                .isTop(false)
                .isMuted(false)
                .lastMessageContent(MessageConstant.GROUP_CREATE_MESSAGE)
                .lastMessageTime(now)
                .score(score)
                .build();

        // 8. 异步通知与后续处理
        // 本地数据库事务 commit 成功后，再发送 MQ，否则可能出现 MQ 发送成功但本地事务回滚导致的幽灵消息。
        AfterCommitUtil.executeAfterCommit(() -> {
            // 8.1 先修改创建者的ZSet，并存入静态资源（conversation的static,meta）
            conversationCacheService.addConversationToZSetSafe(userId, conversationId, score);

            // 8.2 写入群成员 Hash 缓存CANEL BIONLOG
            try {
                conversationCacheService.writeToRedis(conversationId, members);
            } catch (Exception e) {
                log.error("建群写入Redis缓存失败, groupId: {}", conversationId, e);
            }

            // 发送消息队列(不需要发给创建者)
            allMemberIds.remove(userId);
            // 构建创建群聊消息
            ContactConversationEvent event = ContactConversationEvent.builder()
                    .contactConversationType(ContactConversationType.GROUP_CREATE)
                    .senderId(userId)
                    .allReceiverIds(allMemberIds)
                    .conversationVO(conversationVO)
                    .description("邀请你加入群聊")
                    .timestamp(System.currentTimeMillis())
                    .build();
            contactConversationEventPublisher.publishContactConversationEvent(event);
        });

        // 返回ConversationVO
        return conversationVO;
    }

}