package org.cd.zmqnet;

import org.zeromq.ZMQ;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 静态方法
 */
public class ZmqFct {
    private static final   Subscriber subscriber=new Subscriber();
    private static final Publisher publisher=new Publisher();

    private static  IZmqLog iZmqLog=null;//日志接口


    /**
     * 不能发布的地址
     */
    private static final ConcurrentHashMap<String,Long> mapBindUrl=new ConcurrentHashMap<>();


    /**
     * 开启线程绑定日志
     */
    private static volatile  boolean isUnbanded=true;



    /**
     * 设置订阅者分发使用的最大线程数，默认CPU核数*1.5
     * @param num
     */
    public  void setSubMaxThreaNum(int num) {
        subscriber.setMaxThreadNum(num);
    }

    /**
     * 订阅者分发缓存量，缓存超过该值则启动分发线程，默认10000
     * @param num
     */
    public  void  setSubPackNum(int num) {
        subscriber.setPackNum(num);
    }

    /**
     * 设置日志接收
     * @param zmqLog
     */
    public   static void setZmqLog(IZmqLog zmqLog) {
        subscriber.setZmqLog(zmqLog);
        publisher.setZmqLog(zmqLog);
        iZmqLog = zmqLog;
    }


    /**
     *初始化订阅连接地址
     * @param url
     */
    public  static void initSubscriber(String[] url) {
        subscriber.connect(url);
    }

    /**
     * 初始化发布地址
     * @param url
     * @return
     */
    public  static boolean initPusblisherBind(String url) {

        boolean r = publisher.bind(url);
        testPusblisaddress(url, publisher);
        return r;
    }

    /**
     * 初始化发布端口(推荐)
     * @param port 端口
     * @return 成功否
     */
    public  static boolean initPusblisherBind(int port) {

        boolean r = publisher.bind(port);
        testPusblisaddress("tcp://127.0.0.1:" + port, publisher);
        return r;
    }

    /**
     * 初始化发布IP
     * @param ip  ip地址
     * @return
     */
    public  static int initPusblisherIP(String ip) {

        int port = publisher.bindIp(ip);
        testPusblisaddress("tcp://" + ip + ":" + port, publisher);
        return port;
    }


    /**
     * 初始化发布连接中心
     * @param urls 中心地址
     */
    public  static void initPusblisherProxy(String[] urls) {
        publisher.connect(urls);
    }

    /**
     * 测试发布地址是否成功
     */
    private synchronized static void  testPusblisaddress(String url,Publisher thepublisher) {
        url = url.trim().toLowerCase();
        if (!url.startsWith("tcp://")) {
            url = "tcp://" + url;
        }
        if (!mapBindUrl.containsKey(url)) {
            mapBindUrl.put(url, System.currentTimeMillis());
        } else {
            return;//正在测试
        }
        Subscriber tmp = new Subscriber();
        String finalUrl = url;
        tmp.connect(url);
        tmp.subscriber("innertest", (t, d) -> {
            if (t.equals("innertest") || new String(d).equals("innertest")) {
                //成功接收以后释放
                mapBindUrl.remove(finalUrl);
                tmp.unSubscriber(t);//清理匿名类
                tmp.disconnect(finalUrl);
                tmp.close();//关闭
                if(iZmqLog!=null)
                {
                    iZmqLog.add("测试innertest成功并断开:"+finalUrl);
                }
            }
        });
        for (int i = 0; i < 10; i++) {
            ZMQ.sleep(100, TimeUnit.MILLISECONDS);
            thepublisher.publish("innertest", "innertest");
        }
        if (isUnbanded) {
            isUnbanded = false;
            bindLog();
        }
    }

    /**
     * 订阅主题
     * @param topic 主题
     * @param Isubscriber 数据输出接口
     */
    public   static void  subscriber(String topic,IBilSubscriber Isubscriber) {
        subscriber.subscriber(topic, Isubscriber);
    }

    /**
     * 同时订阅多个主题
     * @param topics 主题
     * @param Isubscriber 数据输出接口
     */
    public static void  subscriber(String[] topics,IBilSubscriber Isubscriber) {
        for (String topic : topics
        ) {
            subscriber.subscriber(topic, Isubscriber);
        }

    }

    /**
     * 发布数据
     * @param topic 主题
     * @param data 数据
     */
    public  static void publish(String topic,byte[]data) {
        publisher.publish(topic, data);
    }

    /**
     * 发布数据
     * @param topic 主题
     * @param data 数据
     */

    public  static   void publish(String topic,String data) {
        publisher.publish(topic, data);
    }


    /**
     * 发布端测试日志
     */
    private static void  bindLog() {
        Thread t = new Thread(() -> {
            checkAddress();
            return;
        });
        t.setName("ZmqFctLog");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 发布地址检查日志
     */
  private   static void checkAddress() {
        while (true) {
            ZMQ.sleep(1);
            if (mapBindUrl.isEmpty()) {
                ZMQ.sleep(1);
            }
            for (Map.Entry<String, Long> entry : mapBindUrl.entrySet()) {
                long s = System.currentTimeMillis() - entry.getValue();
                if (s / 1000 > 2 && iZmqLog != null) {
                    iZmqLog.add("发布地址异常，不能发布数据：" + entry.getKey());
                }
            }
        }
    }
}
