/**
 * 功能: 根据不同情况发送验证码
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 下午4:39
 */
package com.telechat.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyCodeDTO {
    private Byte type; // 验证码类型 1:注册 2:登录 3:忘记密码
    private String identifyType; // 验证码识别类型 邮箱
    private String identifier; // 验证码识别符 邮箱
}
