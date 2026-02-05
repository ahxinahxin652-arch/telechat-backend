/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/21 下午5:05
 */
package com.telechat.controller.user;

import com.telechat.constant.ExceptionConstant;
import com.telechat.pojo.cache.UserInfoCache;
import com.telechat.pojo.dto.UserInfoDTO;
import com.telechat.pojo.entity.User;
import com.telechat.pojo.result.Result;
import com.telechat.pojo.vo.UserInfoVO;
import com.telechat.service.UserService;
import com.telechat.util.RedisTemplateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.telechat.exception.exceptions.LoginException;

@RestController
@RequestMapping("/profile")
@Tag(name = "用户信息接口")
public class ProfileController {
    @Autowired
    private UserService userService;

    @Resource
    private RedisTemplateUtil redisTemplateUtil;

    /**
     * 获取个人信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取个人简介")
    @GetMapping
    public Result<UserInfoVO> getUserInfo() {
        try {
            // 从Security上下文中获取用户ID
            Long userId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            if (userId != null) {
                // 读取缓存（内置缓存的完整流程）
                UserInfoCache userInfoCache = redisTemplateUtil.getUserInfoCache(userId);

                UserInfoVO userInfoVO = UserInfoVO.builder()
                        .username(userInfoCache.getUsername())
                        .nickname(userInfoCache.getNickname())
                        .avatar(userInfoCache.getAvatar())
                        .gender(userInfoCache.getGender())
                        .bio(userInfoCache.getBio())
                        .build();
                return Result.success(userInfoVO);
            } else {
                return Result.error(ExceptionConstant.USER_NOT_LOGIN_ERROR_CODE, "用户未登录");
            }
        } catch (Exception e) {
            return Result.error(ExceptionConstant.SERVER_ERROR_CODE, "获取用户信息失败");
        }
    }

    // 更新个人信息（不含头像）
    @Operation(summary = "更新个人信息")
    @PutMapping("/update")
    public Result<String> updateUserInfo(@RequestBody @Valid UserInfoDTO userInfoDTO) {
        try {
            User user = this.getUser();
            userService.updateUserInfo(user, userInfoDTO);
            return Result.success("用户信息更新成功");
        } catch (Exception e) {
            return Result.error(ExceptionConstant.SERVER_ERROR_CODE, "更新用户信息失败: " + e.getMessage());
        }
    }

    // 专门的头像上传接口
    @Operation(summary = "上传头像")
    @PostMapping("/uploadAvatar")
    public Result<String> uploadAvatar(@RequestParam("avatar") MultipartFile avatarFile) {
        try {
            User user = this.getUser();
            String avatarUrl = userService.uploadAvatar(user, avatarFile);
            return Result.success(avatarUrl);
        } catch (Exception e) {
            return Result.error(ExceptionConstant.SERVER_ERROR_CODE, "头像上传失败: " + e.getMessage());
        }
    }

    // 获取用户信息（从headers中获取token）[已校验登录是否异常]
    private User getUser() {
        // 从Security上下文中获取用户ID
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        // 根据用户ID查询用户信息
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new LoginException(ExceptionConstant.USER_NOT_LOGIN_ERROR_CODE, "用户不存在");
        }
        return userService.getUserById(userId);
    }
}
