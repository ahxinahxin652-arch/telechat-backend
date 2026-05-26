package com.telechat.service;

import com.telechat.pojo.dto.LoginDTO;
import com.telechat.pojo.dto.RegisterDTO;
import com.telechat.pojo.dto.ResetPasswordDTO;
import com.telechat.pojo.entity.User;


public interface UserAuthsService {
    /**
     * 校验邮箱是否已存在
     * @param email 邮箱
     * @return true 已存在 false 不存在
     */
    boolean isEmailExist(String email);

    /**
     * 注册
     * @param registerDTO 注册信息
     */
    void register(RegisterDTO registerDTO);

    /**
     * 登录
     *
     * @param loginDTO 登录信息
     * @return token
     */
    String login(LoginDTO loginDTO);

    /**
     * 重置密码
     *
     * @param resetPasswordDTO 重置密码信息
     */
    void resetPassword(ResetPasswordDTO resetPasswordDTO);
}
