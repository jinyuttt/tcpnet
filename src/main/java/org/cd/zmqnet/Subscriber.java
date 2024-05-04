package org.cd.zmqnet;

import org.cd.ThreadManager;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Subscriber implements  ISubscriber{

   // private ZContext context = new ZContext();
    private ZContext context=null;//上下文
    private  ZMQ.Socket socket=null;//zmq
    private  IZmqLog zmqLog=null;//日志

    /**
     * 是否没有关闭
     */
    private volatile  boolean isUnClosed=true;

    /**
     * 需要初始化
     */
    private  volatile  boolean isUnInit =true;

    /**
     * 订阅者
     */
    private final ConcurrentHashMap<String, List<IBilSubscriber>> mapISubscriber=new ConcurrentHashMap<>();

    /**
     * 缓存队列
     */
    private final ConcurrentLinkedQueue<MsgEntity> queue=new ConcurrentLinkedQueue<MsgEntity>();

    /**
     * 积压计数器
     */
    private final AtomicLong packCounter=new AtomicLong(0);

    /**
     * 分发最大线程数
     */
    private  int maxThreadNum =(int)(Runtime.getRuntime().availableProcessors()*1.5);

    /**
     * 开始线程分发的数量
     */
    private  int packNum=10000;

    /**
     * 当前分发线程数
     */
    private final AtomicInteger packThread=new AtomicInteger(0);



    /**
     * 所有主题统计数据
     */
    private final ConcurrentHashMap<String, AtomicLong> mapTopics=new ConcurrentHashMap<>();

    /**
     * 所有地址
     */
    private final ConcurrentHashMap<String, String> mapaddress=new ConcurrentHashMap<>();



    /**
     * 没有连接成功的地址
     */
    private final ConcurrentHashMap<String, String> mapUnAddress=new ConcurrentHashMap<>();


    public  Subscriber() {
        context = ZmqCtx.context;
        socket = context.createSocket(zmq.ZMQ.ZMQ_SUB);
//        socket.setHWM(100000);
//        socket.setLinger(1000);
        socket.setReceiveBufferSize(1024 * 1024);
        socket.setSendBufferSize(1024 * 1024);
        socket.setTCPKeepAlive(1);
        //  socket.setTCPKeepAliveIdle(2 * 60);
        //  socket.setTCPKeepAliveCount(10);
        // 设置发送ZMTP心跳的时间间隔, 单位:ms
        socket.setHeartbeatIvl(5 * 60 * 1000);
        // 设置ZMTP心跳的超时时间, 单位:ms
        socket.setHeartbeatTimeout(60 * 1000);
        // 设置ZMTP心跳的TTL值, 单位:ms
        socket.setHeartbeatTtl(10 * 60 * 1000);

    }

    /**
     * 设置分发使用的最大线程数，默认CPU核数*1.5
     * @param num 值
     */
    public  void setMaxThreadNum(int num)
    {
        this.maxThreadNum =num;
    }

    /**
     * 分发缓存量，缓存超过该值则启动分发线程.默认10000
     * @param num  值
     */
    public  void  setPackNum(int num)
    {
        this.packNum=num;
    }

    /**
     * 设置日志接收
     * @param zmqLog
     */
    public  synchronized void setZmqLog(IZmqLog zmqLog)
    {
        this.zmqLog=zmqLog;
        socket.monitor("inproc://zmqmonitorsub", zmq.ZMQ.ZMQ_EVENT_ALL
        );
        monitorTopic();
    }
    /**
     * 连接地址
     * @param url
     * @return
     */
    public synchronized boolean connect(String url) {
    url = url.toLowerCase().trim();
    if (!url.startsWith("tcp://")) {
        url = "tcp://" + url;
    }
    mapaddress.put(url,"");
    mapUnAddress.put(url,"");
    return socket.connect(url);
}

    /**
     * 连接地址
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
     * 订阅
     * @param topic 主题
     * @param subscriber  订阅返回
     * @return 订阅成功否
     */
    public synchronized  boolean subscriber(String topic, IBilSubscriber subscriber) {
        List<IBilSubscriber> lst = mapISubscriber.get(topic);
        mapTopics.put(topic, new AtomicLong(0));
        if (lst == null) {
            lst = new ArrayList<>();
            mapISubscriber.put(topic, lst);
        }
        if (!lst.contains(subscriber)) {
            lst.add(subscriber);
        }
        if (isUnInit) {
            //同步方法，判断一次
            isUnInit = false;
            recvice();
            pollMsg();
            monitorTopic();
            otherlog();
        }
        return socket.subscribe(topic);
    }

    /**
     * 注销
     * @param topic
     * @return  注销成功否
     */
    public synchronized  boolean unSubscriber(String topic) {
        mapISubscriber.remove(topic);
       return socket.unsubscribe(topic);
    }

    /**
     * 接收数据
     */
    private void  recvice() {
        //一直使用的线程不用线程池
      Thread t=  new Thread(new Runnable() {
            @Override
            public void run() {
                while (isUnClosed) {
                    try {
                        String topic = socket.recvStr();
                        byte[] data = socket.recv();
                       AtomicLong counter= mapTopics.get(topic);
                      if(counter!=null) {
                          //为空说明接收的是相似主题，例如：A，接收到AA
                          counter.incrementAndGet();
                          queue.add(new MsgEntity(topic, data));
                          packCounter.incrementAndGet();//积压总计数
                      }

                    } catch (Exception e) {
                        if (isUnClosed) {
                            //关闭资源造成
                            break;
                        }
                    }
                }
            }
        });
      t.setName("Subscriber");
      t.setDaemon(true);
      t.start();
    }

    /**
     * 分发数据
     */
    private void  pollMsg() {
     Thread t=   new Thread(new Runnable() {
            @Override
            public void run() {
                while (isUnClosed) {

                    if (queue.isEmpty()) {
                        ZMQ.sleep(100, TimeUnit.MILLISECONDS);
                        continue;
                    }
                    MsgEntity msg = queue.poll();
                    if (msg == null) {
                        continue;
                    }
                    packCounter.decrementAndGet();
                    List<IBilSubscriber> lst = mapISubscriber.get(msg.topic);
                    if (lst != null) {
                        for (int i = 0; i < lst.size(); i++) {
                            lst.get(i).add(msg.topic, msg.msg);
                        }
                    }
                    //计算分发，用队列的size影响性能，size方法会遍历队列
                    if (packCounter.get() > packNum && packThread.get() < maxThreadNum) {
                        if (zmqLog != null) {
                            zmqLog.add("开启线程方法：" + packThread.get());
                        }
                        pollPack();
                    }
                }
            }
        });
     t.setName("pollMsg");
     t.setDaemon(true);
     t.start();
    }

    /**
     * 启动线程加入方法
     */
    private void  pollPack() {
        packThread.incrementAndGet();//线程计数
        ThreadManager.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                //低于就退出
                while (packCounter.get() > packNum / 4) {
                    MsgEntity msg = queue.poll();
                    if (msg == null) {
                        break;//其它线程在消费
                    }
                    packCounter.decrementAndGet();//减小计数

                    //遍历返回
                    List<IBilSubscriber> lst = mapISubscriber.get(msg.topic);
                    if (lst != null) {
                        for (int i = 0; i < lst.size(); i++) {
                            lst.get(i).add(msg.topic, msg.msg);
                        }
                    }
                }
                packThread.decrementAndGet();//线程计数
            }
        });
    }

    /**
     * 监视事件
     */
    private  void  monitorTopic() {
       // ZMQ.Socket monitor=null;//监视
        ZMQ.Socket  monitor = context.createSocket(zmq.ZMQ.ZMQ_PAIR);
        monitor.connect("inproc://zmqmonitorsub");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isUnClosed) {
                    //这里使用原来的，新版的方法有卡顿现象
                    zmq.ZMQ.Event event = zmq.ZMQ.Event.read(monitor.base(), ZMQ.NOBLOCK);  //从当前moniter里面读取event
                    if (zmqLog != null&&event!=null) {
                        StringBuffer stringBuffer = new StringBuffer();
                        switch (event.event) {
                            case zmq.ZMQ.ZMQ_EVENT_CONNECTED:
                                stringBuffer.append("订阅端连接地址成功：" + event.addr);
                                mapUnAddress.remove(event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_CLOSED:
                                stringBuffer.append("订阅端关闭：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_DISCONNECTED:
                                stringBuffer.append("订阅端连接地址断开：" + event.addr);
                                mapUnAddress.put(event.addr, "");
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_CONNECT_DELAYED:
                                stringBuffer.append("订阅端同步连接重试：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_CONNECT_RETRIED:
                                stringBuffer.append("订阅端异步连接重试：" + event.addr);
                                break;
                            case zmq.ZMQ.ZMQ_EVENT_ACCEPT_FAILED:
                                stringBuffer.append("订阅端接受连接失败：" + event.addr);
                                break;
                            default:

                        }
                        zmqLog.add(stringBuffer.toString());
                    }
                    if(event==null)
                    {
                        ZMQ.sleep(1);
                    }

                }
                monitor.close();
            }
        }).start();
    }

    /**
     * 订阅日志
     */
    private void  otherlog() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (isUnClosed) {
                    StringBuffer stringBuffer = new StringBuffer();
                    ZMQ.sleep(10);//11秒统计
                    for (String key : mapUnAddress.keySet()
                    ) {
                        socket.connect(key);//连接
                        stringBuffer.append(key);
                        stringBuffer.append(";");
                    }
                    if (stringBuffer.length() > 0) {
                        if (zmqLog != null) {
                            zmqLog.add("未连接地址：" + stringBuffer);
                            stringBuffer.setLength(0);
                        }
                    }
                    for (Map.Entry<String,AtomicLong> entry : mapTopics.entrySet()) {
                        //初始化完成集合大小才能这样遍历
                        stringBuffer.append(entry.getKey());
                        stringBuffer.append("#");
                        stringBuffer.append(entry.getValue());
                        stringBuffer.append(";");
                    }
                    if (stringBuffer.length() > 0) {
                        if (zmqLog != null) {
                            zmqLog.add("接收到主题数据量：" + stringBuffer);
                            stringBuffer.setLength(0);
                        }
                    }
                    if(zmqLog!=null) {
                        zmqLog.add("订阅积压数量：" + packCounter.get());
                    }

                }
            }
        }).start();
    }

    /**
     * 断开连接
     * @param url
     * @return
     */
    public boolean disconnect(String url) {
        return socket.disconnect(url);
    }



    /**
     * 关闭
     */
    public void close()
    {
        isUnClosed=false;
        isUnInit =true;
        socket.close();
        mapTopics.clear();
        mapaddress.clear();
        mapUnAddress.clear();
        mapISubscriber.clear();
        packThread.set(0);
        packCounter.set(0);
    }
}
