package com.telechat.service;

import com.telechat.pojo.dto.UserInfoDTO;
import com.telechat.pojo.entity.User;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    /**
     * 根据用户ID获取用户信息
     *
     * @param userIdFromToken 用户ID
     * @return 用户信息
     */
    User getUserById(Long userIdFromToken);

    /**
     * 更新用户信息
     *
     * @param user        用户
     * @param userInfoDTO 用户信息
     */
    void updateUserInfo(User user, @Valid UserInfoDTO userInfoDTO);

    /**
     * 上传头像
     *
     * @param userId      用户ID
     * @param avatarFile 头像文件
     * @return 头像URL
     */
    String uploadAvatar(User userId, MultipartFile avatarFile);

    /**
     * 根据用户名获取用户ID
     *
     * @param username 用户名
     * @return 用户ID
     */
    Long getUserIdByUsername(String username);
}
