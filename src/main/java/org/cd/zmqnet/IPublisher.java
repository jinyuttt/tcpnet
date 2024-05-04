package org.cd.zmqnet;

/**
 * 发布接口
 */
public interface IPublisher {

    /**
     * 设置日志接口
     * @param zmqLog 日志输出
     */
    void setZmqLog(IZmqLog zmqLog);

    /**
     * 绑定端口（推荐）
     * @param port  端口
     * @return
     */
    boolean bind(int port);

    /**
     * 绑定地址
     * @param url 地址
     * @return
     */
    boolean bind(String url);

    /**
     * 绑定IP
     * @param ip IP地址
     * @return
     */
    int bindIp(String ip);

    /**
     * 连接中心
     * @param url 中心地址
     * @return
     */
    boolean connect(String url);

    /**
     * 连接中心
     * @param urls 中心地址
     */
    void connect(String[] urls);

    /**
     * 发布数据
     * @param topic 主题
     * @param data 数据
     */
    void publish(String topic, byte[] data);

    /**
     * 发布数据
     * @param topic 主题
     * @param data  数据
     */
    void publish(String topic, String data);

    /**
     * 关闭
     */
    void close();

}
