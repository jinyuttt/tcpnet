package org.cd.zmqnet;

public interface ISubscriber {

    /**
     * 设置日
     * @param zmqLog 日志输出
     */
    void setZmqLog(IZmqLog zmqLog);

    /**
     * 连接地址
     * @param url 发布端地址（包括中心）
     * @return
     */
    boolean connect(String url);

    /**
     * 连接地址
     * @param urls 发布端地址（包括中心）
     */
    void  connect(String[]urls);

    /**
     * 订阅主题
     * @param topic 主题
     * @param subscriber 数据接输出
     * @return 订阅成功否
     */
    boolean subscriber(String topic, IBilSubscriber subscriber);

    /**
     * 注销订阅的主题
     * @param topic 主题
     * @return 注销成功否
     */
    boolean unSubscriber(String topic);

    /**
     * 断开连接
     * @param url 端口连接地址
     * @return
     */
    boolean disconnect(String url);

    /**
     * 关闭
     */
    void close();
}
