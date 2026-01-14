/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 下午12:22
 */
package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.telechat.mapper.UserMapper;
import com.telechat.pojo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserDao {
    @Autowired
    private UserMapper userMapper;

    /**
     * 插入用户
     *
     * @param user 用户
     */
    public void insert(User user) {
        // 插入用户
        userMapper.insert(user);
    }

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户
     */
    public User selectById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 更新用户
     *
     * @param user 用户
     */
    public void updateById(User user) {
        userMapper.updateById(user);
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户
     */
    public User selectByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getUsername, username);
        return userMapper.selectOne(queryWrapper);
    }
}
