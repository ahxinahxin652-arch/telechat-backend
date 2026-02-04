package com.telechat.util;

import com.telechat.constant.RedisConstant;
import com.telechat.mapper.dao.ContactDao;
import com.telechat.mapper.dao.UserDao;
import com.telechat.pojo.cache.ContactsCache;
import com.telechat.pojo.cache.UserInfoCache;
import com.telechat.pojo.entity.Contact;
import com.telechat.pojo.entity.User;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTemplateUtil {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ContactDao contactDao;
    private final UserDao userDao;

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
            return (List<ContactsCache>) cacheObj;
        }

        // 3. 缓存未命中 -> 查询数据库
        List<Contact> dbContacts = contactDao.list(userId);

        // 4. 处理数据库为空的情况 (防缓存穿透)
        if (dbContacts == null || dbContacts.isEmpty()) {
            // 存入空列表，过期时间设短一点（例如 5 分钟）
            redisTemplate.opsForValue().set(
                    cacheKey,
                    Collections.emptyList(),
                    5,
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

}
