package com.zd.sccommon.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 * 
 * <p>基于Twitter的Snowflake算法实现分布式唯一ID生成</p>
 * <p>64位ID结构：1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
@Component
public class SnowflakeIdGenerator {

    /**
     * 起始时间戳 (2024-01-01 00:00:00)
     */
    private static final long START_TIMESTAMP = 1704067200000L;

    /**
     * 机器ID位数
     */
    private static final long MACHINE_ID_BITS = 10L;

    /**
     * 序列号位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID最大值
     */
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);

    /**
     * 序列号最大值
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器ID左移位数
     */
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    /**
     * 机器ID
     */
    private final long machineId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     * 
     * @param machineId 机器ID (0-1023)
     */
    public SnowflakeIdGenerator(long machineId) {
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException(
                String.format("机器ID必须在0到%d之间", MAX_MACHINE_ID));
        }
        this.machineId = machineId;
        log.info("雪花算法ID生成器初始化完成，机器ID: {}", machineId);
    }

    /**
     * 默认构造函数，使用默认机器ID
     */
    public SnowflakeIdGenerator(long machineId, long datacenterId) {
        this(1L); // 默认机器ID为1
    }

    /**
     * 生成下一个ID
     * 
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();

        // 时钟回拨检查
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 小幅回拨，等待追上
                try {
                    Thread.sleep(offset << 1);
                    timestamp = getCurrentTimestamp();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(
                            String.format("时钟回拨异常，拒绝生成ID。当前时间戳: %d, 上次时间戳: %d", 
                                        timestamp, lastTimestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待时钟追上时被中断", e);
                }
            } else {
                throw new RuntimeException(
                    String.format("时钟回拨异常，拒绝生成ID。当前时间戳: %d, 上次时间戳: %d", 
                                timestamp, lastTimestamp));
            }
        }

        // 同一毫秒内序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新的毫秒，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装64位ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成字符串格式的ID
     * 
     * @param prefix ID前缀
     * @return 带前缀的字符串ID
     */
    public String nextStringId(String prefix) {
        return prefix + nextId();
    }

    /**
     * 生成文档ID
     * 
     * @return 文档ID，格式：doc_xxxxxxxxx
     */
    public String nextDocumentId() {
        return nextStringId("doc_");
    }

    /**
     * 生成分片ID
     * 
     * @return 分片ID，格式：chunk_xxxxxxxxx
     */
    public String nextChunkId() {
        return nextStringId("chunk_");
    }

    /**
     * 生成笔记ID
     * 
     * @return 笔记ID，格式：note_xxxxxxxxx
     */
    public String nextNoteId() {
        return nextStringId("note_");
    }

    /**
     * 获取当前时间戳
     * 
     * @return 当前时间戳
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 等待下一毫秒
     * 
     * @param lastTimestamp 上次时间戳
     * @return 新的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    /**
     * 解析ID中的时间戳
     * 
     * @param id 雪花算法生成的ID
     * @return 时间戳
     */
    public long parseTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + START_TIMESTAMP;
    }

    /**
     * 解析ID中的机器ID
     * 
     * @param id 雪花算法生成的ID
     * @return 机器ID
     */
    public long parseMachineId(long id) {
        return (id >> MACHINE_ID_SHIFT) & MAX_MACHINE_ID;
    }

    /**
     * 解析ID中的序列号
     * 
     * @param id 雪花算法生成的ID
     * @return 序列号
     */
    public long parseSequence(long id) {
        return id & MAX_SEQUENCE;
    }
}