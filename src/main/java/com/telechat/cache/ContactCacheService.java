package com.telechat.cache;

import com.telechat.cache.entity.ContactsCache;
import com.telechat.constant.RedisConstant;
import com.telechat.mapper.dao.ContactDao;
import com.telechat.pojo.entity.Contact;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContactCacheService {
    // redis
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // dao
    @Resource
    private ContactDao contactDao;

    /**
     * 获取联系人缓存列表
     * <p>逻辑：Redis 有就直接返回；Redis 没有则查 DB，写入 Redis 后返回</p>
     *
     * @param userId 用户ID
     * @return List<ContactsCache> (仅包含关系数据：contactId, friendId, remark)
     */
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
}
