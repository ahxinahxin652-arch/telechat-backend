/**
 * 功能: 邮箱相关异常
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/19 上午11:59
 */
package com.telechat.exception.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
//Lombok的@Data注解默认会生成equals和hashCode方法，并尝试调用父类的相应方法。但由于EmailException继承自RuntimeException（间接继承Object），而RuntimeException没有显式的equals/hashCode实现，导致编译器警告。
//在@Data注解中明确指定callSuper=false参数，告诉Lombok不要调用父类的equals和hashCode方法，因为这是一个异常类，通常不需要考虑父类状态。
public class EmailException extends RuntimeException {

    private int code;
    private String message;

    public EmailException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}