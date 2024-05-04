package org.cd.zmqnet;




import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 发布者
 */
public class Publisher implements  IPublisher{

     private ZContext context=null;//zmq的socket
    private ZMQ.Socket socket=null;//不是线程安全的
    private  IZmqLog zmqLog=null;//日志
    private  ZMQ.Socket monitor=null;//监视

    private volatile  boolean isUnMonitor=true;//控制启动

    private  volatile  boolean isUnClose=true;//是否关闭


    /**
     * 已经开启发布线程
     */
    private volatile  boolean isUnStart =true;

    /**
     * 控制启动
     */
    private ReentrantLock reentrantLock=new ReentrantLock();

    /**
     * 缓存队列
     */
    private final ConcurrentLinkedQueue<MsgEntity> queue=new ConcurrentLinkedQueue<MsgEntity>();
    public Publisher() {
        context = ZmqCtx.context;
        socket = context.createSocket(zmq.ZMQ.ZMQ_PUB);
//        socket.setHWM(100000); 上下文中统一设置
//        socket.setLinger(1000);
        socket.setReceiveBufferSize(1024 * 1024);

        socket.setTCPKeepAlive(1);

        // 设置发送ZMTP心跳的时间间隔, 单位:ms
        socket.setHeartbeatIvl(5 * 60 * 1000);
        // 设置ZMTP心跳的超时时间, 单位:ms
        socket.setHeartbeatTimeout(60 * 1000);
        // 设置ZMTP心跳的TTL值, 单位:ms
        socket.setHeartbeatTtl(10 * 60 * 1000);
    }

    /**
     * 设置日志接口
     * @param zmqLog
     */
    public synchronized void setZmqLog(IZmqLog zmqLog) {
        this.zmqLog = zmqLog;
        socket.monitor("inproc://zmqmonitorpub", ZMQ.EVENT_ALL);
        if(isUnMonitor) {
            //这是同步方法，判断一次即可
            isUnMonitor = false;
            monitorTopic();
        }

    }

    /**
     * 绑定本地端口
     * @param port
     * @return
     */
    public synchronized boolean bind(int port) {
        return this.bind("tcp://*:" + port);
    }

    /**
     * 绑定地址
     * @param url
     * @return
     */
    public synchronized boolean bind(String url) {
        url = url.trim().toLowerCase();
        if (!url.startsWith("tcp://")) {
            url = "tcp://" + url;
        }
        return socket.bind(url);
    }

    /**
     * 绑定随机端口
     * @param ip ip地址
     * @return
     */
    public synchronized int bindIp(String ip) {
        return socket.bindToRandomPort(ip);
    }

    /**
     * 连接中心地址（有中心时）
     * @param url
     * @return
     */
    public synchronized boolean connect(String url) {
        url = url.trim().toLowerCase();
        if (!url.startsWith("tcp://")) {
            url = "tcp://" + url;
        }
        return socket.connect(url);
    }

    /**
     * 连接多个中心（有多个中心时）
     * @param urls
     */
    public synchronized void  connect(String[]urls) {
        if (urls == null) {
            return;
        }
        for (String url : urls
        ) {
            this.connect(url);
        }
    }

    /**
     * 发布数据
     * @param topic
     * @param data
     */
    public void publish(String topic,byte[]data) {
//        socket.sendMore(topic);
//        socket.send(data);
        queue.add(new MsgEntity(topic, data));
        if(isUnStart) {
            isUnStart = false;
            pack();
        }
    }

    /**
     * 发布主题
     * @param topic
     * @param data
     */
    public void  publish(String topic,String data) {
        byte[] buf = data.getBytes(StandardCharsets.UTF_8);
        this.publish(topic, buf);
    }

    /**
     * 发送数据
     */
    private  void  pack() {
        //多线程拿不到锁
        if (reentrantLock.tryLock()) {
            //一直使用的线程不用线程池
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isUnClose) {
                        if (queue.isEmpty()) {
                            ZMQ.sleep(100, TimeUnit.MILLISECONDS);
                        }
                        //
                        MsgEntity msg = queue.poll();
                        if (msg != null) {
                            socket.sendMore(msg.topic);
                            socket.send(msg.msg);
                        }
                    }
                }
            });
            t.setName("publisher");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * 监听事件
     */
    private  void  monitorTopic() {
        monitor = context.createSocket(zmq.ZMQ.ZMQ_PAIR);
        monitor.connect("inproc://zmqmonitorpub");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int count=0;
                while (isUnClose) {
                    //这里使用原来的，新版的方法有卡顿现象
                    //不用阻塞方式，1.方便关闭，不会报错；2.可以利用方法输出其它内容
                  count++;
                    zmq.ZMQ.Event event = zmq.ZMQ.Event.read(monitor.base(), 1);  //从当前moniter里面读取event
                    if (zmqLog != null && event != null) {
                        StringBuffer stringBuffer = new StringBuffer();
                        switch (event.event) {
                            case zmq.ZMQ.ZMQ_EVENT_ACCEPTED:
                                stringBuffer.append("发布端接受地址成功：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_LISTENING:
                                stringBuffer.append("发布端监听成功：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_BIND_FAILED:
                                stringBuffer.append("发布端监听失败：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_ACCEPT_FAILED:
                                stringBuffer.append("发布端接受连接失败：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_CONNECTED:
                                stringBuffer.append("发布端连接地址成功：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_CLOSED:
                                stringBuffer.append("发布端关闭：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_DISCONNECTED:
                                stringBuffer.append("发布端连接地址断开：" + event.addr);

                                break;
                            case zmq.ZMQ.ZMQ_EVENT_CONNECT_DELAYED:
                                stringBuffer.append("发布端同步连接重试：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_CONNECT_RETRIED:
                                stringBuffer.append("发布端异步连接重试：" + event.addr);
                                break;
                            default:

                        }
                        zmqLog.add(stringBuffer.toString());
                    }
                    if (event == null) {
                        ZMQ.sleep(1);
                    }
                    if(zmqLog!=null&&count%10==0) {
                        //发送端没有那么大量，不用计数方式,如果事件不是频繁则基本10秒一次
                        zmqLog.add("发布积压数量:" + queue.size());
                    }

                }
            }
        });
        t.setName("pubmonitorTopic");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 关闭
     */
    public  void  close() {
        isUnClose = false;
        socket.close();
    }
}
