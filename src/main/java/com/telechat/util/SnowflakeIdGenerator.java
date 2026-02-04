package com.telechat.util;

/**
 * 雪花算法 ID 生成器
 *
 * 结构（64bit）：
 * 0 - 41bit 时间戳 - 5bit 数据中心 - 5bit 机器号 - 12bit 序列号
 */
public class SnowflakeIdGenerator {

    /** 起始时间戳（2024-01-01 00:00:00） */
    private static final long START_TIMESTAMP = 1704038400000L;

    /** 各部分位数 */
    private static final long SEQUENCE_BITS = 12;
    private static final long WORKER_BITS = 5;
    private static final long DATACENTER_BITS = 5;

    /** 最大值 */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_BITS);

    /** 位移 */
    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId out of range");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个 ID（线程安全）
     */
    public synchronized long nextId() {
        long currentTimestamp = currentTime();

        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards, refusing to generate id");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        return (currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = currentTime();
        }
        return currentTimestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }
}
