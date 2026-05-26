package com.telechat.cache;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.telechat.cache.entity.*;
import com.telechat.constant.ExceptionConstant;
import com.telechat.constant.RedisConstant;
import com.telechat.constant.ServiceConstant;
import com.telechat.exception.exceptions.ConversationException;
import com.telechat.mapper.dao.ConversationDao;
import com.telechat.mapper.dao.ConversationMemberDao;
import com.telechat.pojo.entity.Conversation;
import com.telechat.pojo.entity.ConversationMember;
import com.telechat.pojo.enums.ConversationType;
import com.telechat.util.RedisTemplateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationCacheService {
    // redis
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisTemplateUtil redisTemplateUtil;

    // service
    @Resource
    private UserInfoCacheService userInfoCacheService;

    // dao
    @Resource
    private ConversationDao conversationDao;

    @Resource
    private ConversationMemberDao conversationMemberDao;

    // Lua脚本：EXISTS 检查是否存在 -> 存在则 ZADD -> EXPIRE 续期 -> 返回 1; 不存在直接返回 0
    public final static String luaScript =
            "if redis.call('EXISTS', KEYS[1]) == 1 then " +
                    "   redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2]); " +
                    "   redis.call('EXPIRE', KEYS[1], ARGV[3]); " + // 顺手续期
                    "   return 1; " +
                    "else " +
                    "   return 0; " +
                    "end";

    // Lua脚本：EXISTS 检查是否存在 -> 存在则 ZREM 删除成员 -> EXPIRE 续期 -> 返回删除的数量; 不存在直接返回 0
    public final static String removeLuaScript =
            "if redis.call('EXISTS', KEYS[1]) == 1 then " +
                    "   local removed = redis.call('ZREM', KEYS[1], ARGV[1]); " +
                    "   redis.call('EXPIRE', KEYS[1], ARGV[2]); " + // 顺手续期活跃用户的缓存，防雪崩
                    "   return removed; " +
                    "else " +
                    "   return 0; " +
                    "end";

    /**
     *
     * 批量写入redis群成员数据
     * @param groupId 会话ID
     * @param members 用户会话
     * */
    public void writeToRedis(Long groupId, List<ConversationMember> members) {
        // 1. 参数校验

        String cacheKey = RedisConstant.CONVERSATION_GROUP_MEMBER + groupId;

        // 2. 批量写入redis
        // 将 List<ConversationMember> 转换为 Map<String, GroupMemberCache>
        Map<String, GroupMemberCache> memberCacheHashMap = members.stream()
                .collect(Collectors.toMap(
                        member -> member.getUserId().toString(), // Key：用户ID转为String，保证序列化兼容性
                        member -> {
                            // Value：将数据库实体转换为缓存实体
                            GroupMemberCache cache = new GroupMemberCache();
                            // 建议使用 BeanUtils 拷贝属性，或者手动 set
                            // 注意：这里只拷贝业务需要的字段，保持缓存轻量
                            BeanUtils.copyProperties(member, cache);
                            return cache;
                        },
                        (existing, replacement) -> replacement // 如果有重复Key（理论上不应该），保留最新的
                ));
        redisTemplate.opsForHash().putAll(cacheKey, memberCacheHashMap);
        // 过期时间：3天
        redisTemplate.expire(cacheKey, RedisConstant.CONVERSATION_GROUP_MEMBER_DURATION, TimeUnit.DAYS);
    }

    /**
     *
     * 获取redis群成员数据
     * <p>
     * */
    private Map<Long, GroupMemberCache> readFromRedis(Long groupId) {
        return Collections.emptyMap();
    }

    /**
     * 安全地向用户的会话 ZSet 中添加新会话
     * 使用 Lua 脚本保证：仅当 Key 存在时才添加，并顺手重置过期时间防雪崩
     */
    public void addConversationToZSetSafe(Long userId, Long conversationId, Double score) {
        String cacheKey = RedisConstant.USER_CONVERSATIONS_ZSET + userId;

        // 基础时间 24小时 + 随机 0-2 小时防止缓存雪崩
        long ttl = 86400 + new Random().nextInt(7200);

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        // 执行 Lua 脚本，一次网络往返解决所有问题，绝对的并发安全
        redisTemplate.execute(redisScript,
                Collections.singletonList(cacheKey),
                score,
                conversationId,
                ttl);
    }

    /**
     * 安全地从用户的会话 ZSet 中删除会话
     * 使用 Lua 脚本保证原子性：仅当 Key 存在时才删除，并顺手重置过期时间防雪崩
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     */
    public void removeConversationFromZSetSafe(Long userId, Long conversationId) {
        // 参数校验
        if (userId == null || conversationId == null) {
            return;
        }

        String cacheKey = RedisConstant.USER_CONVERSATIONS_ZSET + userId;

        // 基础时间 24小时 + 随机 0-2 小时防止缓存雪崩
        long ttl = 86400 + new Random().nextInt(7200);

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(removeLuaScript);
        // 脚本返回 Long 类型（删除的个数 1 或 0）
        redisScript.setResultType(Long.class);

        // 执行 Lua 脚本
        // 【避坑指南】：因为预热(preHeat)时存入的是 String.valueOf() 的字节码，
        // 这里 ARGV[1] 务必传入 String.valueOf(conversationId)，否则 ZREM 匹配不到对象！
        redisTemplate.execute(redisScript,
                Collections.singletonList(cacheKey),
                conversationId,
                ttl);
    }

    /**
     * 会话数据列表预热
     * 场景：用户登录后，先预热出前60条会话关系
     *
     * @param userId 用户ID
     * @return true: 预热成功/false: 预热失败
     */
    public Boolean preHeatConversationZSets(Long userId) {
        String cacheKey = RedisConstant.USER_CONVERSATIONS_ZSET + userId;

        // 1. 检查 Key 是否存在 (高性能操作)
        Boolean hasKey = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(hasKey)) {
            // 如果缓存已存在，直接返回，无需增量补齐
            return true;
        }

        // 2. 缓存预热（200个会话）
        List<ConversationZSetCache> dbList = conversationDao.selectConversationIdsByUserId(userId, ServiceConstant.PREHEAT_COUNT);

        if (CollectionUtils.isEmpty(dbList)) {
            return true;  // todo 后续修改逻辑：每个新用户必有一个好友：自己 || lastMessageTime为空
        }

        // 3. 使用 Pipeline 批量写入，减少网络 RTT
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] rawKey = cacheKey.getBytes();

            for (ConversationZSetCache conversationZSetCache : dbList) {
                // 计算 Score: 如果置顶则加偏移量
                double score = conversationZSetCache.getLastMessageTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (conversationZSetCache.isToped()) {
                    score += ServiceConstant.TOP_SCORE_OFFSET;
                }

                // ZADD key score member
                connection.zAdd(rawKey, score, String.valueOf(conversationZSetCache.getConversationId()).getBytes());
            }

            // 4. 设置过期时间，防止冷用户占用内存，同时通过随机偏移防止雪崩
            // 基础时间 24小时 + 随机 0-2 小时
            long ttl = 86400 + new Random().nextInt(7200);
            connection.expire(rawKey, ttl);
            return null;
        });
        return true;
    }


    /**
     * 获取某个会话静态缓存信息（私聊的缓存信息不用获取）
     * 场景：查看群聊基本信息
     *
     * @param conversationId 会话ID
     */
    public ConversationStaticInfoCache getConversationStaticInfoCache(Long conversationId) {
        String cacheKey = RedisConstant.CONVERSATION_STATIC_INFO + conversationId;

        // 1. 查缓存
        Object cacheObj = redisTemplate.opsForValue().get(cacheKey);
        // 1.1 缓存命中
        if (cacheObj != null) {
            ConversationStaticInfoCache conversationStaticInfoCache = (ConversationStaticInfoCache) cacheObj;
            // 看是否为空对象标记，是则直接返回null，防止缓存穿透
            if(conversationStaticInfoCache.isNullPlaceholder()) {
                return null;
            }
            return conversationStaticInfoCache;
        }

        // 1.2 缓存未命中
        // 回源数据库
        Conversation conversation = conversationDao.selectById(conversationId);

        // 空数据缓存不存在的空对象，防止缓存穿透
        if (conversation == null) {
            // 【防缓存穿透】这种单信息缓存逻辑大致相同，参考UserInfo
            ConversationStaticInfoCache nullCache = ConversationStaticInfoCache.builder()
                    .id(-1L) // 标记位
                    .build();

            // 写入Redis
            redisTemplate.opsForValue().set(
                    cacheKey,
                    nullCache,
                    RedisConstant.EMPTY_DATA,
                    TimeUnit.MINUTES);

            return null;
        }

        // 正常缓存数据
        ConversationStaticInfoCache conversationStaticInfoCache = ConversationStaticInfoCache.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .title(conversation.getTitle())
                .avatar(conversation.getAvatar())
                .ownerId(conversation.getOwnerId())
                .status(conversation.getStatus())
                .build();

        redisTemplate.opsForValue().set(
                cacheKey,
                conversationStaticInfoCache,
                RedisConstant.CONVERSATION_STATIC_INFO_DURATION,
                TimeUnit.MINUTES
        );
        return conversationStaticInfoCache;
    }

    /**
     * 批量获取会话静态缓存信息
     * 场景：批量加载会话，查询会话
     * 策略：Redis MultiGet -> 过滤未命中 -> DB BatchQuery -> Redis MultiSet -> 批量获取私聊目标用户信息 -> 动态组装结果
     *
     * @param userId 当前用户ID (用于判断私聊对方是谁)
     * @param conversationIds 会话ID集合
     * @return Map<Long, ConversationStaticInfoCache> key: conversationId, value: cacheObj
     */
    public Map<Long, ConversationStaticInfoCache> getConversationStaticInfoCacheMapByIds(Long userId, Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 去重并准备 Redis Keys (保持顺序)
        List<Long> distinctIds = new ArrayList<>(new HashSet<>(conversationIds));
        List<String> keys = distinctIds.stream()
                .map(id -> RedisConstant.CONVERSATION_STATIC_INFO + id)
                .toList();

        // 2. Redis 管道批量读取 (Pipeline/MultiGet)
        List<Object> cacheResults = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, ConversationStaticInfoCache> resultMap = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        // 3. 整理缓存命中结果
        for (int i = 0; i < distinctIds.size(); i++) {
            Long convId = distinctIds.get(i);
            Object result = cacheResults != null ? cacheResults.get(i) : null;

            if (result instanceof ConversationStaticInfoCache) {
                resultMap.put(convId, (ConversationStaticInfoCache) result);
            } else {
                missingIds.add(convId); // 记录未命中的 ID
            }
        }

        // 4. 处理未命中的数据 (回源数据库)
        if (!missingIds.isEmpty()) {
            // 4.1 数据库批量查询
            List<Conversation> dbConversations = conversationDao.selectBatchIds(missingIds);

            if (dbConversations != null && !dbConversations.isEmpty()) {
                Map<String, Object> writeToRedisMap = new HashMap<>();

                for (Conversation conversation : dbConversations) {
                    // 【核心优化1】: 缓存的必须是纯净的、与查询者无关的静态实体信息！
                    // 私聊的 title 和 avatar 属于动态视图数据，不能入全局缓存
                    ConversationStaticInfoCache cacheDTO = ConversationStaticInfoCache.builder()
                            .id(conversation.getId())
                            .type(conversation.getType())
                            .title(conversation.getTitle())    // 如果是私聊，DB里本身就是 null
                            .avatar(conversation.getAvatar())  // 如果是私聊，DB里本身就是 null
                            .ownerId(conversation.getOwnerId())
                            .status(conversation.getStatus())
                            .uniqueKey(conversation.getUniqueKey()) // 【重点】务必将 uniqueKey 也缓存下来，后续组装需要
                            .build();

                    // 加入结果集
                    resultMap.put(conversation.getId(), cacheDTO);
                    // 准备写入 Redis
                    writeToRedisMap.put(RedisConstant.CONVERSATION_STATIC_INFO + conversation.getId(), cacheDTO);
                }

                // 4.3 Redis 批量回写
                redisTemplate.opsForValue().multiSet(writeToRedisMap);

                // 4.4 异步或循环设置过期时间 (加上随机值防雪崩)
                writeToRedisMap.keySet().forEach(key ->
                        redisTemplateUtil.expireWithRandom(key, RedisConstant.CONVERSATION_STATIC_INFO_DURATION, TimeUnit.MINUTES)
                );
            }
        }

        // 5. 【核心优化2】: 批量处理私聊动态信息（解决 N+1 问题）
        Set<Long> friendIdsToFetch = new HashSet<>();
        Map<Long, Long> convToFriendIdMap = new HashMap<>();

        // 5.1 第一遍遍历：收集所有私聊会话中，需要查询的对方(好友)ID
        for (ConversationStaticInfoCache cacheObj : resultMap.values()) {
            if (cacheObj != null && ConversationType.PRIVATE.equals(cacheObj.getType())) {
                String uniqueKey = cacheObj.getUniqueKey();
                if (uniqueKey != null && uniqueKey.startsWith("P:")) {
                    // 解析 P:uid1_uid2
                    String[] parts = uniqueKey.substring(2).split("_");
                    if (parts.length == 2) {
                        Long uid1 = Long.valueOf(parts[0]);
                        Long uid2 = Long.valueOf(parts[1]);

                        // 判断谁是当前用户的好友
                        Long friendId = userId.equals(uid1) ? uid2 : uid1;
                        friendIdsToFetch.add(friendId);
                        convToFriendIdMap.put(cacheObj.getId(), friendId);
                    }
                }
            }
        }

        // 5.2 批量拉取所有好友的用户信息 (利用你已经写好的批量获取函数)
        if (!friendIdsToFetch.isEmpty()) {
            Map<Long, UserInfoCache> userInfoMap = userInfoCacheService.getUserInfoCacheMapByIds(friendIdsToFetch);

            // 5.3 第二遍遍历：动态将对方的名字和头像，装配到返回给当前请求的 DTO 中
            for (Map.Entry<Long, Long> entry : convToFriendIdMap.entrySet()) {
                Long convId = entry.getKey();
                Long friendId = entry.getValue();
                UserInfoCache friendInfo = userInfoMap.get(friendId);

                if (friendInfo != null) {
                    ConversationStaticInfoCache cacheObj = resultMap.get(convId);
                    // 注意：这里修改的是反序列化后生成的对象，仅仅作用于当前线程的返回结果，不会修改 Redis 里的数据
                    cacheObj.setTitle(friendInfo.getNickname());
                    cacheObj.setAvatar(friendInfo.getAvatar());
                }
            }
        }

        return resultMap;
    }

    /**
     * 获取某个会话动态信息缓存
     * 场景：
     *
     * @param conversationId 会话ID
     */
    public ConversationMetaInfoCache getConversationMetaInfoCache(Long conversationId) {
        String cacheKey = RedisConstant.CONVERSATION_META_INFO + conversationId;

        // 1. 查缓存
        Object cacheObj = redisTemplate.opsForValue().get(cacheKey);
        // 1.1 缓存命中
        if (cacheObj != null) {
            ConversationMetaInfoCache conversationMetaInfoCache = (ConversationMetaInfoCache) cacheObj;
            // 看是否为空对象标记，是则直接返回null，防止缓存穿透
            if(conversationMetaInfoCache.isNullPlaceholder()) {
                return null;
            }
            return conversationMetaInfoCache;
        }

        // 1.2 缓存未命中
        // 回源数据库
        Conversation conversation = conversationDao.selectById(conversationId);

        // 空数据缓存不存在的空对象，防止缓存穿透
        if (conversation == null) {
            // 【防缓存穿透】这种单信息缓存逻辑大致相同，参考UserInfo
            ConversationMetaInfoCache nullCache = ConversationMetaInfoCache.builder()
                    .id(-1L) // 标记位
                    .build();

            // 写入Redis
            redisTemplate.opsForValue().set(
                    cacheKey,
                    nullCache,
                    RedisConstant.EMPTY_DATA,
                    TimeUnit.MINUTES);

            return null;
        }

        // 正常缓存数据
        ConversationMetaInfoCache conversationMetaInfoCache = ConversationMetaInfoCache.builder()
                .id(conversation.getId())
                .lastMessageId(conversation.getLastMessageId())
                .lastMessageContent(conversation.getLastMessageContent())
                .lastMessageTime(conversation.getLastMessageTime())
                .build();

        redisTemplate.opsForValue().set(
                cacheKey,
                conversationMetaInfoCache,
                RedisConstant.CONVERSATION_META_INFO_DURATION,
                TimeUnit.MINUTES
        );
        return conversationMetaInfoCache;
    }

    /**
     * 批量获取会话动态缓存信息
     * 场景：批量加载会话
     * 策略：Redis MultiGet -> 过滤未命中 -> DB BatchQuery -> Redis MultiSet -> 合并结果
     *
     * @param conversationIds 会话ID集合
     * @return Map<Long, ConversationInfoCache> key: conversationId, value: cacheObj
     */
    public Map<Long, ConversationMetaInfoCache> getConversationMetaInfoCacheMapByIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 准备 Redis Keys (保持顺序)
        List<Long> distinctIds = new ArrayList<>(conversationIds);
        List<String> keys = distinctIds.stream()
                .map(id -> RedisConstant.CONVERSATION_META_INFO + id)
                .toList();

        // 2. Redis 管道批量读取 (Pipeline/MultiGet)
        List<Object> cacheResults = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, ConversationMetaInfoCache> resultMap = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        // 3. 整理缓存命中结果
        for (int i = 0; i < distinctIds.size(); i++) {
            Long uid = distinctIds.get(i);
            assert cacheResults != null;
            Object result = cacheResults.get(i);

            if (result instanceof ConversationMetaInfoCache) {
                resultMap.put(uid, (ConversationMetaInfoCache) result);
            } else {
                missingIds.add(uid); // 记录未命中的 ID
            }
        }

        // 4. 处理未命中的数据 (回源数据库)
        if (!missingIds.isEmpty()) {
            // 4.1 数据库批量查询
            List<Conversation> dbConversations = conversationDao.selectBatchIds(missingIds);

            if (dbConversations != null && !dbConversations.isEmpty()) {
                Map<String, Object> writeToRedisMap = new HashMap<>();

                for (Conversation conversation : dbConversations) {
                    // 4.2 Entity -> CacheDTO 转换
                    ConversationMetaInfoCache cacheDTO = ConversationMetaInfoCache.builder()
                            .id(conversation.getId())
                            .lastMessageId(conversation.getLastMessageId())
                            .lastMessageContent(conversation.getLastMessageContent())
                            .lastMessageTime(conversation.getLastMessageTime())
                            .build();

                    // 加入结果集
                    resultMap.put(conversation.getId(), cacheDTO);

                    // 准备写入 Redis
                    writeToRedisMap.put(RedisConstant.CONVERSATION_META_INFO + conversation.getId(), cacheDTO);
                }

                // 4.3 Redis 批量回写
                redisTemplate.opsForValue().multiSet(writeToRedisMap);

                // 4.4 异步或循环设置过期时间 (因为 multiSet 不支持过期)
                // 过期时间加上随机值，防止雪崩
                writeToRedisMap.keySet().forEach(key ->
                        redisTemplateUtil.expireWithRandom(key, RedisConstant.CONVERSATION_META_INFO_DURATION, TimeUnit.MINUTES)
                );
            }
        }

        return resultMap;
    }

    /**
     * 更新会话动态信息缓存
     * 场景：私聊/群聊/频道有人发送新消息时
     *
     * @param conversationMetaInfoCache 会话动态信息缓存
     */
    public void updateConversationMetaInfoCache(ConversationMetaInfoCache conversationMetaInfoCache) {
        // 校验conversationId
        if (conversationMetaInfoCache == null) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code,ExceptionConstant.Judge_Query_Exception_MSG);
        }

        String cacheKey = RedisConstant.CONVERSATION_META_INFO + conversationMetaInfoCache.getId();

        // 1. 组装需要更新的增量Map
        Map<String, String> updateMap = new HashMap<>();
        updateMap.put("lastMessageId", String.valueOf(conversationMetaInfoCache.getLastMessageId()));

        if(conversationMetaInfoCache.getLastMessageContent() != null) {
            updateMap.put("lastMessageContent", conversationMetaInfoCache.getLastMessageContent());

        }
        if(conversationMetaInfoCache.getLastMessageTime() != null) {
            updateMap.put("lastMessageTime", String.valueOf(conversationMetaInfoCache.getLastMessageTime()));
        }

        // 2. 将增量字段覆盖，存在更新就字段，不存在就新建一个key
        redisTemplate.opsForHash().putAll(cacheKey, updateMap);

        // 3. 动态续期，保证活跃的群聊命中率
        redisTemplateUtil.expireWithRandom(cacheKey, RedisConstant.CONVERSATION_META_INFO_DURATION, TimeUnit.MINUTES);
    }

    /**
     * 获取某个会话成员信息缓存
     * 场景：点开会话详细信息
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    public ConversationMemberCache getConversationMemberCache(Long userId, Long conversationId) {
        // 参数校验
        if (userId == null || conversationId == null) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code,ExceptionConstant.Judge_Query_Exception_MSG);
        }

        // 1. 组装 Redis Hash 的 Key 和 Field
        String cacheKey = RedisConstant.USER_CONVERSATION_MEMBER + userId;
        String hashField = String.valueOf(conversationId);

        // 2. 从 Redis Hash 中查询单个会话状态
        Object cacheObj = redisTemplate.opsForHash().get(cacheKey, hashField);

        // 2.1 缓存命中
        if (cacheObj != null) {
            // 将 JSON 字符串反序列化为对象
            ConversationMemberCache memberCache = JSON.parseObject(cacheObj.toString(), ConversationMemberCache.class);

            // 【防缓存穿透】看是否为空对象标记 (-1)
            if(memberCache != null && memberCache.isNullPlaceholder()) {
                return null; // 确实不在这个群里/或者已被踢出
            }
            return memberCache;
        }

        // 2.2 缓存未命中 -> 回源数据库 (必须查 conversation_member 表)
        ConversationMember member = conversationMemberDao.selectByConversationIdAndUserId(conversationId, userId);

        // 3. 处理空数据 (用户不在该会话中)
        if (member == null) {
            // 【防缓存穿透】存入带有 -1 标记位的空对象 JSON
            ConversationMemberCache nullCache = ConversationMemberCache.builder()
                    .id(-1L)
                    .build();

            redisTemplate.opsForHash().put(cacheKey, hashField, JSON.toJSONString(nullCache));

            // 防止缓存雪崩
            redisTemplateUtil.expireWithRandom(cacheKey, RedisConstant.EMPTY_DATA, TimeUnit.MINUTES);
            return null;
        }

        // 4. 正常缓存数据转换与装配
        // 注意：这里需要根据你数据库实体 (ConversationMember) 到 缓存DTO (ConversationMemberCache) 进行映射
        ConversationMemberCache memberCache = ConversationMemberCache.builder()
                .id(member.getId())
                .conversationId(member.getConversationId())
                .userId(member.getUserId())
                .isMuted(member.isMuted())
                .isToped(member.isToped())
                .lastReadMessageId(member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId())
                // 未读数通常由缓存层通过消息累加单独维护，回源查库时可能为0，具体看你业务规划
                .unreadCount(0)
                // 假设你的实体里有 joinedTime
                // .joinedTime(member.getJoinedTime())
                .build();

        // 5. 写入 Redis Hash
        redisTemplate.opsForHash().put(cacheKey, hashField, JSON.toJSONString(memberCache));

        // 6. 动态续期保活
        // 只要用户点开了这个群，就代表他是活跃的，将他的所有群聊状态(Hash)过期时间重置
        redisTemplateUtil.expireWithRandom(cacheKey, RedisConstant.USER_CONVERSATION_MEMBER_DURATION, TimeUnit.MINUTES);

        return memberCache;
    }

    /**
     * 批量获取用户会话状态
     * 场景：登录后加载会话列表和懒加载会话列表
     */
    public Map<Long, ConversationMemberCache> getConversationMemberCacheByIds(Long userId, List<Long> convIds) {
        // 参数校验
        if (convIds == null || convIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String redisKey = RedisConstant.USER_CONVERSATION_MEMBER + userId;

        // 1. 从 Redis 批量获取 (HMGET)
        List<Object> fields = convIds.stream().map(String::valueOf).collect(Collectors.toList());
        List<Object> cachedValues = redisTemplate.opsForHash().multiGet(redisKey, fields);

        Map<Long, ConversationMemberCache> result = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        // 2. 识别缺失的 ID 和 拦截空对象
        for (int i = 0; i < convIds.size(); i++) {
            Long convId = convIds.get(i);
            Object val = cachedValues.get(i);

            if (val != null) {
                ConversationMemberCache cacheObj = JSON.parseObject((String) val, ConversationMemberCache.class);
                // 过滤防穿透的空对象标记 (-1)
                if (cacheObj != null && !cacheObj.isNullPlaceholder()) {
                    result.put(convId, cacheObj);
                }
            } else {
                missingIds.add(convId);
            }
        }

        // 3. 如果有缺失，从 DB 批量补全
        if (!missingIds.isEmpty()) {
            // 联合 userId 和 convIds 查询
            List<ConversationMember> members = conversationMemberDao.selectByUserIdAndConvIds(userId, missingIds);

            // 将查出的结果转为 Map，方便按 convId 查找
            Map<Long, ConversationMember> dbMemberMap = members.stream()
                    .collect(Collectors.toMap(ConversationMember::getConversationId, m -> m));

            Map<String, String> toCache = new HashMap<>();

            // 【安全核心】必须遍历 missingIds，而不是遍历 members
            // 这样才能发现哪些 convId 是数据库里也没有的
            for (Long missingConvId : missingIds) {
                ConversationMember m = dbMemberMap.get(missingConvId);

                if (m != null) {
                    // 3.1 正常数据转换
                    ConversationMemberCache cacheObj = ConversationMemberCache.builder()
                            .id(m.getId())
                            .conversationId(m.getConversationId())
                            .userId(m.getUserId())
                            .isMuted(m.isMuted())
                            .isToped(m.isToped())
                            .lastReadMessageId(m.getLastReadMessageId())
                            .unreadCount(0) // todo 未读数后续单独聚合
                            // .joinedTime(m.getJoinedTime())
                            .build();

                    result.put(missingConvId, cacheObj);
                    toCache.put(String.valueOf(missingConvId), JSON.toJSONString(cacheObj));
                } else {
                    // 3.2 【防缓存穿透】数据库里也没有，说明用户已不在该会话
                    ConversationMemberCache nullCache = ConversationMemberCache.builder()
                            .id(-1L) // 标记位
                            .build();
                    // 存入 Redis 防止恶意刷接口，但不放入 result 返回给前端
                    toCache.put(String.valueOf(missingConvId), JSON.toJSONString(nullCache));
                }
            }

            // 4. 批量写入缓存 (HMSET)
            if (!toCache.isEmpty()) {
                redisTemplate.opsForHash().putAll(redisKey, toCache);
                // 只要访问过，就给整个用户的状态 Hash 续期
                redisTemplateUtil.expireWithRandom(redisKey, RedisConstant.USER_CONVERSATION_MEMBER_DURATION, TimeUnit.MINUTES);
            }
        }

        return result;
    }
}
