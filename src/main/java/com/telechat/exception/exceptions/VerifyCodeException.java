/**
 * 功能: 验证码相关异常
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 下午12:01
 */
package com.telechat.exception.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class VerifyCodeException extends RuntimeException {
    private int code;
    private String message;

    public VerifyCodeException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}