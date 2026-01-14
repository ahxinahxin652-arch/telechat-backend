/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/30 下午8:57
 */
package com.telechat.constant;

public class RedisConstant {
    public static final String USER_TOKEN = "user:token:";

    // 用户信息缓存
    public static final String USER_INFO = "user:info:";
    public static final Long USER_INFO_DURATION = 30L; //30min

    // 用户联系人信息缓存
    public static final String USER_CONTACTS_INFO = "user:contacts:";
    public static final Long USER_CONTACTS_INFO_DURATION = 30L; //30min
}
