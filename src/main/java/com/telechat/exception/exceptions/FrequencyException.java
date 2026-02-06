package com.telechat.exception.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class FrequencyException extends RuntimeException{

    private int code;
    private String message;

    public FrequencyException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
