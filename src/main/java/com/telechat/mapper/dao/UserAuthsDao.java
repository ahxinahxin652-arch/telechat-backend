/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 上午11:11
 */
package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.telechat.mapper.UserAuthsMapper;
import com.telechat.pojo.entity.UserAuths;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Component
public class UserAuthsDao {

    @Autowired
    private UserAuthsMapper userAuthsMapper;

    /**
     * 根据邮箱查询用户凭证
     * @param email 邮箱
     * @return 用户凭证
     */
    public UserAuths selectByEmail(String email) {
        LambdaQueryWrapper<UserAuths> queryWrapper = Wrappers.lambdaQuery(UserAuths.class)
                .eq(UserAuths::getIdentityType, "email")
                .eq(UserAuths::getIdentifier, email);
        return userAuthsMapper.selectOne(queryWrapper);
    }

    /**
     * 插入用户凭证
     * @param userAuths 用户凭证
     */
    public void insert(UserAuths userAuths) {
        // 插入用户凭证
        userAuthsMapper.insert(userAuths);
    }


    /**
     * 根据用户ID更新用户凭证
     * @param userAuths 用户凭证
     */
    public void updateById(UserAuths userAuths) {
        // 更新用户凭证
        userAuthsMapper.updateById(userAuths);
    }
}