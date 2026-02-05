/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/30 下午8:57
 */
package com.telechat.constant;

public class RedisConstant {
    public static final String USER_TOKEN = "user:token:";

    // 空信息，防止缓存穿透
    public static final Long EMPTY_DATA = 5L;

    // 用户信息缓存
    public static final String USER_INFO = "user:info:";
    public static final Long USER_INFO_DURATION = 30L; //30min

    // 用户联系人信息缓存
    public static final String USER_CONTACTS_INFO = "user:contacts:";
    public static final Long USER_CONTACTS_INFO_DURATION = 30L; //30min

    // 用户联系人申请缓存
    public static final String USER_CONTACTS_APPLY = "user:contactApplies:";
    public static final Long USER_CONTACT_APPLIES_DURATION = 60L;
}
