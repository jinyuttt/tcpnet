package org.cd.zmqnet;

/**
 * 接收接口
 */
public interface IBilSubscriber {
    void add(String topic,byte[]data);
}
