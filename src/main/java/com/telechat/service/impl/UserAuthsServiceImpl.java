/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 上午10:11
 */
package com.telechat.service.impl;

import com.telechat.constant.ExceptionConstant;
import com.telechat.constant.MessageConstant;
import com.telechat.exception.exceptions.EmailException;
import com.telechat.exception.exceptions.RegisterException;
import com.telechat.exception.exceptions.VerifyCodeException;
import com.telechat.mapper.dao.UserAuthsDao;
import com.telechat.mapper.dao.UserDao;
import com.telechat.pojo.dto.LoginDTO;
import com.telechat.pojo.dto.RegisterDTO;
import com.telechat.pojo.dto.ResetPasswordDTO;
import com.telechat.pojo.entity.User;
import com.telechat.pojo.entity.UserAuths;
import com.telechat.security.JwtTokenProvider;
import com.telechat.service.UserAuthsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserAuthsServiceImpl implements UserAuthsService {
    @Autowired
    private UserAuthsDao userAuthsDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 校验邮箱是否已存在
     *
     * @param email 邮箱
     * @return true 已存在 false 不存在
     */
    @Override
    public boolean isEmailExist(String email) {
        // 查询该邮箱是否存在
        UserAuths userAuths = userAuthsDao.selectByEmail(email);
        return userAuths != null;
    }

    /**
     * 注册
     *
     * @param registerDTO 注册信息
     */
    @Transactional
    @Override
    public void register(RegisterDTO registerDTO) {
        // 先判断是哪种注册方式
        switch (registerDTO.getIdentifyType()) {
            case "email":
                // 判断邮箱是否已经存在
                if (this.isEmailExist(registerDTO.getIdentifier())) {
                    throw new EmailException(ExceptionConstant.EMAIL_EXIST_CODE, "邮箱已存在");
                }
                else{
                    // 验证码校验
                    // 从redis中获取验证码
                    String redisVerifyCodeKey = "register:" + registerDTO.getIdentifier();
                    String verifyCode = (String) redisTemplate.opsForValue().get(redisVerifyCodeKey);
                    if (verifyCode == null || !verifyCode.equals(registerDTO.getVerifyCode())) {
                        throw new VerifyCodeException(ExceptionConstant.VERIFY_CODE_ERROR_CODE, "验证码错误");                    }
                    // 注册成功
                    // 添加用户信息
                    // 随机生成username
                    String username = "user_" + (int) (Math.random() * 1000000000);
                    // 检查username是否存在
                    while (userDao.selectByUsername(username) != null) {
                        username = "user_" + (int) (Math.random() * 1000000000);
                    }
                    User user = User.builder()
                            .username(username)
                            .nickname(registerDTO.getIdentifier())
                            .avatar(MessageConstant.DEFAULT_AVATAR)
                            .gender((byte) 0)
                            .bio("该用户还没有介绍自己哦")
                            .status((byte) 1)
                            .createTime(LocalDateTime.now())
                            .updateTime(LocalDateTime.now())
                            .lastLoginTime(LocalDateTime.now())
                            .build();
                    userDao.insert(user);

                    //添加用户凭证
                    UserAuths userAuths = UserAuths.builder()
                            .userId(user.getId())
                            .identityType("email")
                            .identifier(registerDTO.getIdentifier())
                            .credential(null) // 邮箱不需要凭证,除非用户登录后自行设置了登录密码
                            .build();
                    userAuthsDao.insert(userAuths);

                    redisTemplate.delete(registerDTO.getIdentifier()); // 注册成功后删除验证码
                }
                break;
            default:
                throw new RegisterException(ExceptionConstant.REGISTER_TYPE_ERROR_CODE, "注册类型错误");
        }
    }

    /**
     * 邮箱验证码登录
     *
     * @param loginDTO 登录信息
     * @return JWT Token
     */
    @Override
    public String login(LoginDTO loginDTO) {
        switch (loginDTO.getIdentifyType()) {
            case "email":
                // 验证码校验
                // 从redis中获取验证码
                String redisVerifyCodeKey = "login:" + loginDTO.getIdentifier();
                String verifyCode = (String) redisTemplate.opsForValue().get(redisVerifyCodeKey);
                if (verifyCode == null || !verifyCode.equals(loginDTO.getVerifyCode())) {
                    throw new VerifyCodeException(ExceptionConstant.VERIFY_CODE_ERROR_CODE, "验证码错误");
                }
                // 查询用户凭证是否存在
                UserAuths userAuths = userAuthsDao.selectByEmail(loginDTO.getIdentifier());
                if (userAuths == null) {
                    throw new EmailException(ExceptionConstant.NOT_EXIST_CODE, "用户不存在，请先注册");
                }

                // 查询用户信息
                User userTmp = userDao.selectById(userAuths.getUserId());
                if (userTmp == null) {
                    throw new EmailException(ExceptionConstant.NOT_EXIST_CODE, "用户信息异常");
                }
                if (userTmp.getStatus() == 0) {
                    throw new EmailException(ExceptionConstant.NOT_ALLOWED_CODE, "该账号暂时被冻结，无法登录");
                }


                // 更新最后登录时间
                userTmp.setLastLoginTime(LocalDateTime.now());
                userDao.updateById(userTmp);
                // 生成JWT Token，包含用户角色信息
                String token = jwtTokenProvider.generateToken(userTmp.getId(), userTmp.getNickname(), MessageConstant.USER_ROLE);

                // 删除已使用的验证码
                redisTemplate.delete(redisVerifyCodeKey);

                return token;
            default:
                throw new RegisterException(ExceptionConstant.REGISTER_TYPE_ERROR_CODE, "登录类型错误");
        }
    }

    /**
     * 重置密码
     *
     * @param resetPasswordDTO 重置密码信息
     */
    @Override
    public void resetPassword(ResetPasswordDTO resetPasswordDTO) {
        switch (resetPasswordDTO.getIdentifyType()) {
            case "email":
                // 验证码校验
                // 从redis中获取验证码
                String redisVerifyCodeKey = "resetPassword:" + resetPasswordDTO.getIdentifier();
                String verifyCode = (String) redisTemplate.opsForValue().get(redisVerifyCodeKey);
                if (verifyCode == null || !verifyCode.equals(resetPasswordDTO.getVerifyCode())) {
                    throw new VerifyCodeException(ExceptionConstant.VERIFY_CODE_ERROR_CODE, "验证码错误");
                }
                // 查询用户凭证是否存在
                UserAuths userAuths = userAuthsDao.selectByEmail(resetPasswordDTO.getIdentifier());
                if (userAuths == null) {
                    throw new EmailException(ExceptionConstant.NOT_EXIST_CODE, "用户不存在，请先注册");
                }
                // 更新密码
                userAuths.setCredential(resetPasswordDTO.getPassword());
                userAuthsDao.updateById(userAuths);
                // 删除已使用的验证码
                redisTemplate.delete(resetPasswordDTO.getIdentifier());
                break;
            default:
                throw new RegisterException(ExceptionConstant.ResetPassword_TYPE_ERROR_CODE, "重置密码类型错误");
        }
    }

}
