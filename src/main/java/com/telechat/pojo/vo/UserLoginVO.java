/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 下午6:15
 */
package com.telechat.pojo.vo;


import com.telechat.pojo.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Builder
public class UserLoginVO {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserInfoVO profile;
}
