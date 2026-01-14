/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/18 下午3:11
 */
package com.telechat.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

@TableName("user")
public class User {
    @TableId( value = "id", type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private byte gender;
    private String bio;
    private byte status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
}
