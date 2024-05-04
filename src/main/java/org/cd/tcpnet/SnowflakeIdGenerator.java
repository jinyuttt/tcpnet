package org.cd.tcpnet;


/**
 * 雪花算法
 */
public class SnowflakeIdGenerator {
    private final long twepoch = 1288834974657L; // 起始时间戳（毫秒）
    private final long workerIdBits = 5L; // 工作机器ID所占的位数
    private final long datacenterIdBits = 5L; // 数据中心ID所占的位数
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits); // 最大工作机器ID
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits); // 最大数据中心ID
    private final long sequenceBits = 12L; // 序列号所占的位数
    private final long workerIdShift = sequenceBits; // 工作机器ID左移位数
    private final long datacenterIdShift = sequenceBits + workerIdBits; // 数据中心ID左移位数
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits; // 时间戳左移位数
    private final long sequenceMask = -1L ^ (-1L << sequenceBits); // 序列号掩码

    private long workerId; // 工作机器ID
    private long datacenterId; // 数据中心ID
    private long sequence = 0L; // 序列号
    private long lastTimestamp = -1L; // 上一次时间戳

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("workerId can't be greater than %d or less than 0");
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than %d or less than 0");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }

    // 测试用例
    public static void main(String[] args) {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);
        for (int i = 0; i < 10; i++) {
            System.out.println(idGenerator.nextId());
        }
    }
}
