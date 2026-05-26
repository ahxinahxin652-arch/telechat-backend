package com.telechat.cache;

import com.telechat.cache.entity.UserInfoCache;
import com.telechat.constant.RedisConstant;
import com.telechat.mapper.dao.UserDao;
import com.telechat.pojo.entity.User;
import com.telechat.util.RedisTemplateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserInfoCacheService {
    // redis
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisTemplateUtil redisTemplateUtil;

    // dao
    @Resource
    private UserDao userDao;

    /**
     * 获取用户信息缓存
     * 场景：查看个人信息，查看他人信息
     *
     * @param userId 用户ID
     * @return UserInfoCache
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
     * 批量获取用户缓存信息
     * 场景：批量获取联系人信息，批量获取群成员信息
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
                redisTemplate.opsForValue().multiSet(writeToRedisMap);

                // 4.4 异步或循环设置过期时间 (因为 multiSet 不支持过期)
                // 过期时间加上随机值，防止雪崩
                writeToRedisMap.keySet().forEach(key ->
                        redisTemplateUtil.expireWithRandom(key, RedisConstant.USER_INFO_DURATION, TimeUnit.MINUTES)
                );
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
}
