/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 下午6:24
 */
package com.telechat.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResetPasswordDTO {
    private String identifyType;
    private String identifier;
    private String verifyCode;
    private String password;
}
