package com.telechat.util;

import com.telechat.constant.RedisConstant;
import com.telechat.mapper.dao.ContactApplyDao;
import com.telechat.mapper.dao.ContactDao;
import com.telechat.mapper.dao.UserDao;
import com.telechat.pojo.cache.ContactApplyCache;
import com.telechat.pojo.cache.ContactsCache;
import com.telechat.pojo.cache.UserInfoCache;
import com.telechat.pojo.entity.Contact;
import com.telechat.pojo.entity.ContactApply;
import com.telechat.pojo.entity.User;
import com.telechat.pojo.enums.ContactApplyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTemplateUtil {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ContactDao contactDao;
    private final ContactApplyDao contactApplyDao;
    private final UserDao userDao;

    //------------------------------------------------个人信息缓存相关方法------------------------------------------------------------------
    /**
     * 获取用户信息缓存
     * 场景：查看个人信息，查看他人信息
     *
     * @param userId 用户ID
     */
    public UserInfoCache getUserInfoCache(Long userId) {
        String cacheKey = RedisConstant.USER_INFO + userId;

        // 1. 查缓存
        Object cacheObj = redisTemplate.opsForValue().get(cacheKey);

        if (cacheObj != null) {
            UserInfoCache userCache = (UserInfoCache) cacheObj;
            // 【关键判断】如果是空对象标记，直接返回 null，不再查库
            if (userCache.isNullPlaceholder()) {
                return null; // 拦截
            }
            return userCache;
        }

        // 查库
        User user = userDao.selectById(userId);

        // 空数据缓存不存在的空对象，防止缓存穿透
        if (user == null) {
            // 【防穿透关键】构建一个 ID 为 -1 的空对象
            UserInfoCache nullCache = UserInfoCache.builder()
                    .userId(-1L) // 标记位
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
        UserInfoCache userInfoCache = UserInfoCache.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .bio(user.getBio())
                .build();

        redisTemplate.opsForValue().set(
                cacheKey,
                userInfoCache,
                RedisConstant.USER_INFO_DURATION,
                TimeUnit.MINUTES
        );
        return userInfoCache;
    }

    /**
     * 批量获取用户缓存信息（核心优化方法）
     * 策略：Redis MultiGet -> 过滤未命中 -> DB BatchQuery -> Redis MultiSet -> 合并结果
     *
     * @param userIds 用户ID集合
     * @return Map<Long, UserInfoCache> key: userId, value: cacheObj
     */
    public Map<Long, UserInfoCache> getUserInfoCacheMapByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 准备 Redis Keys (保持顺序)
        List<Long> distinctIds = new ArrayList<>(userIds);
        List<String> keys = distinctIds.stream()
                .map(id -> RedisConstant.USER_INFO + id)
                .toList();

        // 2. Redis 管道批量读取 (Pipeline/MultiGet)
        List<Object> cacheResults = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, UserInfoCache> resultMap = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        // 3. 整理缓存命中结果
        for (int i = 0; i < distinctIds.size(); i++) {
            Long uid = distinctIds.get(i);
            assert cacheResults != null;
            Object result = cacheResults.get(i);

            if (result instanceof UserInfoCache) {
                resultMap.put(uid, (UserInfoCache) result);
            } else {
                missingIds.add(uid); // 记录未命中的 ID
            }
        }

        // 4. 处理未命中的数据 (回源数据库)
        if (!missingIds.isEmpty()) {
            // 4.1 数据库批量查询 (只会执行 1 条 SQL: SELECT * FROM user WHERE id IN (...))
            List<User> dbUsers = userDao.selectBatchIds(missingIds);

            if (dbUsers != null && !dbUsers.isEmpty()) {
                Map<String, Object> writeToRedisMap = new HashMap<>();

                for (User user : dbUsers) {
                    // 4.2 Entity -> CacheDTO 转换
                    UserInfoCache cacheDTO = UserInfoCache.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .nickname(user.getNickname())
                            .avatar(user.getAvatar())
                            .gender(user.getGender())
                            .bio(user.getBio())
                            .build();

                    // 加入结果集
                    resultMap.put(user.getId(), cacheDTO);

                    // 准备写入 Redis
                    writeToRedisMap.put(RedisConstant.USER_INFO + user.getId(), cacheDTO);
                }

                // 4.3 Redis 批量回写
                if (!writeToRedisMap.isEmpty()) {
                    redisTemplate.opsForValue().multiSet(writeToRedisMap);

                    // 4.4 异步或循环设置过期时间 (因为 multiSet 不支持过期)
                    // 建议过期时间：24小时 + 随机值，防止雪崩
                    writeToRedisMap.keySet().forEach(key ->
                            redisTemplate.expire(key, RedisConstant.USER_INFO_DURATION, TimeUnit.HOURS)
                    );
                }
            }
        }

        return resultMap;
    }

    /**
     * 删除用户个人信息缓存
     * 场景：用户修改个人信息
     *
     * @param userId 用户ID
     */
    public void deleteUserInfoCache(Long userId) {
        String cacheKey = RedisConstant.USER_INFO + userId;
        redisTemplate.delete(cacheKey);
        log.info("已清除用户 {} 的个人信息缓存", userId);
    }

    //------------------------------------------------联系人缓存相关方法------------------------------------------------------------------
    /**
     * 获取联系人缓存列表
     * <p>逻辑：Redis 有就直接返回；Redis 没有则查 DB，写入 Redis 后返回</p>
     *
     * @param userId 用户ID
     * @return List<ContactsCache> (仅包含关系数据：contactId, friendId, remark)
     */
    @SuppressWarnings("unchecked")
    public List<ContactsCache> getContactCache(Long userId) {
        String cacheKey = RedisConstant.USER_CONTACTS_INFO + userId;

        // 1. 尝试从 Redis 读取
        Object cacheObj = redisTemplate.opsForValue().get(cacheKey);

        // 2. 缓存命中
        if (cacheObj != null) {
            // 如果缓存的是空列表（防止穿透的空值），直接返回空集合
            if (cacheObj instanceof List && ((List<?>) cacheObj).isEmpty()) {
                return Collections.emptyList();
            }
            // todo 如何解决
            return (List<ContactsCache>) cacheObj;
        }

        // 3. 缓存未命中 -> 查询数据库
        List<Contact> dbContacts = contactDao.list(userId);

        // 4. 处理数据库为空的情况 (防缓存穿透)
        if (dbContacts == null || dbContacts.isEmpty()) {
            // 存入空列表，过期时间设短一点
            redisTemplate.opsForValue().set(
                    cacheKey,
                    Collections.emptyList(),
                    RedisConstant.EMPTY_DATA,
                    TimeUnit.MINUTES
            );
            return Collections.emptyList();
        }

        // 5. 实体转换 Entity -> Cache
        // 我们只缓存“关系数据”，不缓存“用户详情(头像/昵称)”，因为用户详情变动频繁且独立
        List<ContactsCache> cacheList = dbContacts.stream().map(c -> ContactsCache.builder()
                .contactId(c.getId())
                .friendId(c.getFriendId()) // 这里的 friendId 就是 userId 对应的那个朋友
                .remark(c.getRemark())
                .build()
        ).collect(Collectors.toList());

        // 6. 写入缓存 (正常过期时间，例如 30 分钟)
        redisTemplate.opsForValue().set(
                cacheKey,
                cacheList,
                RedisConstant.USER_CONTACTS_INFO_DURATION,
                TimeUnit.MINUTES
        );

        return cacheList;
    }

    /**
     * 删除单人联系人缓存
     * 场景：修改备注、删除好友
     *
     * @param userId 用户ID
     */
    public void deleteContactCache(Long userId) {
        String cacheKey = RedisConstant.USER_CONTACTS_INFO + userId;
        redisTemplate.delete(cacheKey);
        log.info("已清除用户 {} 的联系人列表缓存", userId);
    }

    /**
     * 批量删除联系人缓存
     * 场景：添加好友（需要同时删除双方的缓存）
     *
     * @param userIds 用户ID集合
     */
    public void deleteContactCache(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;

        List<String> keys = userIds.stream()
                .map(id -> RedisConstant.USER_CONTACTS_INFO + id)
                .collect(Collectors.toList());

        redisTemplate.delete(keys);
        log.info("已批量清除用户缓存: {}", userIds);
    }

    //------------------------------------------------联系人申请缓存相关方法------------------------------------------------------------------
    /**
     * 获取联系人申请缓存
     * 场景：点开联系人申请
     *
     * @param userId 用户ID（有多少人加这个用户）
     */
    public List<ContactApplyCache> getContactApplyCache(Long userId) {
        String cacheKey = RedisConstant.USER_CONTACTS_APPLY + userId;

        // 1. 读取缓存，获取联系人申请列表
        Object cacheObj = redisTemplate.opsForValue().get(cacheKey);

        // 1.1 命中缓存，直接获取联系人申请列表
        if (cacheObj != null) {
            // 如果缓存的是空列表（防止穿透的空值），直接返回空集合
            if(cacheObj instanceof List && ((List<?>) cacheObj).isEmpty()) {
                return Collections.emptyList();
            }
            // 返回数据
            // todo 如何解决
            return (List<ContactApplyCache>) cacheObj;
        }

        // 1.2 未命中缓存，回源数据库并写入缓存
        List<ContactApply> dbContactApplies = contactApplyDao.selectApplyList(userId, ContactApplyStatus.PENDING);

        // 1.2.1 处理数据库为空的情况（防止缓存穿透）
        if(dbContactApplies == null || dbContactApplies.isEmpty()) {
            // 存入空列表
            redisTemplate.opsForValue().set(
                    cacheKey,
                    Collections.emptyList(),
                    RedisConstant.EMPTY_DATA,
                    TimeUnit.MINUTES
            );
            return Collections.emptyList();
        }

        // 2. 实体转换
        List<ContactApplyCache> cacheList = dbContactApplies.stream().map(c -> ContactApplyCache.builder()
                .contactApplyId(c.getId())
                .userId(c.getUserId()) // 是userId 申请加 friendId好友
                .status(ContactApplyStatus.PENDING.getDesc())
                .createdTime(c.getCreatedTime())
                .build()
        ).collect(Collectors.toList());

        // 3. 写入缓存
        redisTemplate.opsForValue().set(
                cacheKey,
                cacheList,
                RedisConstant.USER_CONTACT_APPLIES_DURATION,
                TimeUnit.MINUTES
        );
        return cacheList;
    }

    /**
     * 删除单人联系人申请缓存
     * 场景：处理申请，某人向该用户发起申请
     *
     * @param userId 用户ID
     */
    public void deleteContactApplyCache(Long userId) {
        String cacheKey = RedisConstant.USER_CONTACTS_APPLY + userId;
        redisTemplate.delete(cacheKey);
        log.info("已清除用户 {} 的联系人申请列表缓存", userId);
    }
}
