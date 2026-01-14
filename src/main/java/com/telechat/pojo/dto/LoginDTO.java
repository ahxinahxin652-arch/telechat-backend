/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 下午12:48
 */
package com.telechat.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    private String identifyType;
    private String identifier;
    private String verifyCode;
}
