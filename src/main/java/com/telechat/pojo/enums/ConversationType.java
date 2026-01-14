package com.telechat.pojo.enums;

import lombok.Data;


public enum ConversationType {
    PRIVATE(1,"PRIVATE"),
    GROUP(2,"GROUP"),
    CHANNEL(3,"CHANNEL");

    private int code;
    private String name;

    ConversationType(int code, String name) {
    }
    public static String getName(int code) {
        for (ConversationType value : ConversationType.values()) {
            if (value.code == code) {
                return value.name;
            }
        }
        return null;
    }

}
