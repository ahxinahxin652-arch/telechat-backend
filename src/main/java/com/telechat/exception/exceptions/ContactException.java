/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午9:26
 */
package com.telechat.exception.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ContactException extends RuntimeException{

    private int code;
    private String message;

    public ContactException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
