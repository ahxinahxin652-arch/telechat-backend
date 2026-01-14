/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 下午6:33
 */
package com.telechat.service.impl;

import com.telechat.constant.MessageConstant;
import com.telechat.mapper.dao.UserDao;
import com.telechat.pojo.dto.UserInfoDTO;
import com.telechat.pojo.entity.User;
import com.telechat.service.UserService;
import com.telechat.util.AliOssUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private UserDao userDao;

    /**
     * 根据用户ID获取用户信息
     *
     * @param userIdFromToken 用户ID
     * @return 用户信息
     */
    @Override
    public User getUserById(Long userIdFromToken) {
        return userDao.selectById(userIdFromToken);
    }

    /**
     * 更新用户信息
     *
     * @param user        用户信息
     * @param userInfoDTO 用户更新信息
     */
    @Override
    public void updateUserInfo(User user, @Valid UserInfoDTO userInfoDTO) {
        user.setNickname(userInfoDTO.getNickname());
        user.setGender(userInfoDTO.getGender());
        user.setBio(userInfoDTO.getBio());

        userDao.updateById(user);
    }

    /**
     * 上传头像
     *
     * @param user    用户ID
     * @param avatarFile 头像文件
     * @return 头像URL
     */
    @Override
    public String uploadAvatar(User user, MultipartFile avatarFile) {
        // 获取用户原头像并删除(不删除默认头像)
        String oldAvatar = user.getAvatar();
        if (oldAvatar != null && !oldAvatar.equals(MessageConstant.DEFAULT_AVATAR)) {
            aliOssUtil.deleteByUrl(oldAvatar);
        }
        String avatarUrl = aliOssUtil.uploadFile(avatarFile);
        user.setAvatar(avatarUrl);

        userDao.updateById(user);
        return avatarUrl;
    }

    /**
     * 根据用户名获取用户ID
     *
     * @param username 用户名
     * @return 用户ID
     */
    @Override
    public Long getUserIdByUsername(String username) {
        User user = userDao.selectByUsername(username);
        if (user == null) {
            return null;
        }
        return user.getId();
    }
}
