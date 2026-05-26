package com.telechat.cache;

import com.telechat.cache.entity.ContactApplyCache;
import com.telechat.constant.RedisConstant;
import com.telechat.mapper.dao.ContactApplyDao;
import com.telechat.pojo.entity.ContactApply;
import com.telechat.pojo.enums.ContactApplyStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContactApplyCacheService {
    // redis
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // dao
    @Resource
    private ContactApplyDao contactApplyDao;

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
            if (cacheObj instanceof List && ((List<?>) cacheObj).isEmpty()) {
                return Collections.emptyList();
            }
            // 返回数据
            // todo 如何解决
            return (List<ContactApplyCache>) cacheObj;
        }

        // 1.2 未命中缓存，回源数据库并写入缓存
        List<ContactApply> dbContactApplies = contactApplyDao.selectApplyList(userId, ContactApplyStatus.PENDING);

        // 1.2.1 处理数据库为空的情况（防止缓存穿透）
        if (dbContactApplies == null || dbContactApplies.isEmpty()) {
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
                .status(ContactApplyStatus.PENDING)
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
