/**
 * 功能: 注册相关异常
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 下午12:04
 */
package com.telechat.exception.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class LoginException extends RuntimeException {

    private int code;
    private String message;

    public LoginException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}