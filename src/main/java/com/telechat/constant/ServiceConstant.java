package com.telechat.constant;

public class ServiceConstant {
    // 预热会话列表200条
    public final static Integer PREHEAT_COUNT = 40;

    // 默认每次懒加载20条会话
    public final static Integer LOAD_CONVERSATION_COUNT = 20;

    // 置顶权重偏移量 (10的13次方，确保加上后一定大于普通的毫秒级时间戳)
    public static final Double TOP_SCORE_OFFSET = 10000000000000D;

    // 9999年时间戳
    public static final Double MAX_TIME = 253402300799000D;
}
