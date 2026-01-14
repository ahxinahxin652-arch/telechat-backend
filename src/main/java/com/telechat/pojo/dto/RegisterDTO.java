/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/18 下午7:36
 */
package com.telechat.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {
    private String identifyType;
    private String identifier;
    private String verifyCode;
}
