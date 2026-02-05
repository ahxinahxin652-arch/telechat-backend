/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/18 下午7:18
 */
package com.telechat.controller.user;

import com.telechat.constant.ExceptionConstant;
import com.telechat.pojo.dto.LoginDTO;
import com.telechat.pojo.dto.RegisterDTO;
import com.telechat.pojo.dto.ResetPasswordDTO;
import com.telechat.pojo.dto.VerifyCodeDTO;
import com.telechat.pojo.entity.User;
import com.telechat.pojo.result.Result;
import com.telechat.pojo.vo.UserInfoVO;
import com.telechat.pojo.vo.UserLoginVO;
import com.telechat.security.JwtTokenProvider;
import com.telechat.service.UserAuthsService;
import com.telechat.service.UserService;
import com.telechat.util.EmailUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户接口")
public class UserController {
    @Autowired
    private EmailUtil emailUtil;
    @Autowired
    private UserAuthsService userAuthsService;
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 认证管理器
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 发送验证码
     *
     * @param verifyCodeDTO 验证码信息
     * @return 发送结果
     */
    @Operation(summary = "发送验证码")
    @PostMapping("/sendVerifyCode")
    public Result<String> sendVerifyCode(@RequestBody VerifyCodeDTO verifyCodeDTO) {
        String type;
        switch (verifyCodeDTO.getType()) {
            case 1:
                type = "register";
                break;
            case 2:
                type = "login";
                break;
            case 3:
                type = "findPassword";
                break;
            default:
                return Result.error(ExceptionConstant.NOT_EXIST_CODE, "不存在的验证码类型");
        }

        String redisVerifyCodeKey  = type+":"+verifyCodeDTO.getIdentifier() ;
        // 查找验证码是否存在且剩余时间是否小于4分钟(防止用户重复发送)
        Long expire = redisTemplate.getExpire(redisVerifyCodeKey, TimeUnit.SECONDS);
        if (expire != null && expire > 240) {
            log.info("验证码已存在，请{}秒后重试", expire - 240);
            return Result.error(ExceptionConstant.VERIFY_CODE_EXIST_CODE, "请" + (expire - 240) + "秒后重试");
        }

        // 发送对应的验证码
        switch (verifyCodeDTO.getIdentifyType()) {
            case "email":
                // 注册
                if(type.equals("register")){
                    // 判断邮箱是否已经存在
                    if (userAuthsService.isEmailExist(verifyCodeDTO.getIdentifier())) {
                        return Result.error(ExceptionConstant.EMAIL_EXIST_CODE, "邮箱已存在");
                    }
                }
                // 登录
                if(type.equals("login") || type.equals("findPassword")){
                    if (!userAuthsService.isEmailExist(verifyCodeDTO.getIdentifier())) {
                        return Result.error(ExceptionConstant.NOT_EXIST_CODE, "邮箱不存在");
                    }
                }
                // 生成验证码
                String verifyCode = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
                // 发送验证码
                emailUtil.sendSimpleMail(verifyCodeDTO.getIdentifier(), "验证码", "您的验证码为：" + verifyCode + "，5分钟内有效");
                // 验证码存入redis
                redisTemplate.opsForValue().set(redisVerifyCodeKey, verifyCode, 5, TimeUnit.MINUTES);
                break;
            default:
                return Result.error(ExceptionConstant.NOT_EXIST_CODE, "识别类型错误");
        }
        return Result.success();
    }

    /**
     * 注册
     *
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        log.info("注册参数：{}", registerDTO);
        // 注册
        userAuthsService.register(registerDTO);
        return Result.success("注册成功");
    }


    /**
     * 验证码登录
     *
     * @param loginDTO 登录信息
     * @return JWT Token
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody LoginDTO loginDTO) {
        log.info("登录参数：{}", loginDTO);
        try {
            User user;
            // 登录并获取JWT Token
            String token = userAuthsService.login(loginDTO);
            user = userService.getUserById(jwtTokenProvider.getUserIdFromToken(token));
            UserInfoVO userInfoVO = UserInfoVO.builder()
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .gender(user.getGender())
                    .bio(user.getBio())
                    .build();
            UserLoginVO userLoginVO = new UserLoginVO(token, "Bearer", jwtTokenProvider.getJwtExpiration(), userInfoVO);
            return Result.success(userLoginVO);
        } catch (Exception e) {
            return Result.error(ExceptionConstant.LOGIN_ERROR_CODE, e.getMessage());
        }
    }

    /**
     * 重新设置密码
     *
     * @param resetPasswordDTO 重置密码信息
     * @return Result<String>
     */
    @PostMapping("/resetPassword")
    public Result<String> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        log.info("重置密码参数：{}", resetPasswordDTO);
        // 重置密码
        userAuthsService.resetPassword(resetPasswordDTO);
        return Result.success("重置密码成功");
    }
}