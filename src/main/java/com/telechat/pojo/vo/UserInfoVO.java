/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/21 下午9:19
 */
package com.telechat.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Builder
public class UserInfoVO {
    private String username;
    private String nickname;
    private String avatar;
    private Byte gender;
    private String bio;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
}
