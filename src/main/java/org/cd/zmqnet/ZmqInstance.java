package org.cd.zmqnet;

import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 订阅发布单例（推荐ZmqFct）
 */
public class ZmqInstance {
    private ZmqInstance(){}

    //静态内部类
    private static class SingletonHolder{
        private static final ZmqInstance INSTANCE = new ZmqInstance();
    }

    //对外提供静态方法获取该对象
    public static ZmqInstance getInstance(){
        return SingletonHolder.INSTANCE;
    }

    private final   Subscriber subscriber=new Subscriber();
    private  final Publisher publisher=new Publisher();

    private IZmqLog iZmqLog=null;//日志接口


    /**
     * 多个发布
     */
    private  final ConcurrentHashMap<String,MultQueue> mapMutQueue=new ConcurrentHashMap<>();

    /**
     * 过滤多个发布地址
     */
    private final List<String> lstMuUrls=new ArrayList<>();

    /**
     * 过滤多个发布主题
     */
    private final List<String> lstMuTopic=new ArrayList<>();



    /**
     * 已经绑定
     */
    private volatile   boolean isBound =false;

    /**
     * 已经开启发布线程
     */
    private volatile  boolean isStartCache =true;

    private final  ReentrantLock  reentrantLock=new ReentrantLock();

    /**
     * 不能发布的地址
     */
    private  final ConcurrentHashMap<String,Long> mapBindUrl=new ConcurrentHashMap<>();


    /**
     * 开启线程绑定日志
     */
    private volatile  boolean isUnbanded=true;


    /**
     * 缓存队列
     */
    private final ConcurrentLinkedQueue<MsgEntity> queue=new ConcurrentLinkedQueue<MsgEntity>();

    /**
     * 设置订阅者分发使用的最大线程数，默认CPU核数*1.5
     * @param num
     */
    public synchronized void setSubMaxThreaNum(int num)
    {
        subscriber.setMaxThreadNum(num);
    }

    /**
     * 订阅者分发缓存量，缓存超过该值则启动分发线程，默认10000
     * @param num
     */
    public synchronized void  setSubPackNum(int num)
    {
        subscriber.setPackNum(num);
    }

    /**
     * 设置日志接收
     * @param zmqLog
     */
    public synchronized void setZmqLog(IZmqLog zmqLog)
    {
        subscriber.setZmqLog(zmqLog);
        publisher.setZmqLog(zmqLog);
        this.iZmqLog=zmqLog;
    }


    /**
     *初始化订阅连接地址
     * @param url
     */
    public synchronized void initSubscriber(String[] url)
    {
         subscriber.connect(url);
    }

    /**
     * 初始化发布地址
     * @param url
     * @return
     */
    public synchronized boolean initPusblisherBind(String url)
    {
        isBound =true;
        boolean r= publisher.bind(url);
        testPusblisaddress(url,publisher);
        return r;
    }

    /**
     * 初始化发布端口(推荐)
     * @param port
     * @return
     */
    public synchronized boolean initPusblisherBind(int port)
    {
        this.isBound =true;
        boolean r= publisher.bind(port);
        testPusblisaddress("tcp://127.0.0.1:"+port,publisher);
        return r;
    }

    /**
     * 初始化发布IP
     * @param ip
     * @return
     */
    public synchronized int initPusblisherIP(String ip)
    {
        isBound =true;
        int port= publisher.bindIp(ip);
        testPusblisaddress("tcp://"+ip+":"+port,publisher);
        return  port;
    }

    /**
     * 一般不使用，只在多网卡在不同局域网情况才使用（与PublishMut方法使用）
     * 订阅不用，订阅直接连接不同地址即可
     * @param topics
     * @param url
     * @throws TcpZmqException
     */
    public synchronized  void initMutPublisher(String[] topics,String url) throws TcpZmqException {
        //
        url=url.trim().toLowerCase();
        if(!url.startsWith("tcp://"))
        {
            url="tcp://"+url;
        }
        if(lstMuUrls.contains(url))
        {
            throw  new TcpZmqException("重复绑定地址");
        }
        //检查主题是否重复
        for (String key:topics
             ) {
            if(lstMuTopic.contains(key))
            {
                throw  new TcpZmqException("重复发布相同主题，不能区分");
            }
        }
        lstMuUrls.add(url);
        Publisher publishert=new Publisher();
        publishert.bind(url);
        if(url.contains("tcp://*:"))
        {
            //处理一下IP;
            url=url.replace("*","127.0.0.1");
        }
        testPusblisaddress(url,publishert);
        MultQueue multQueue=new MultQueue(publishert);
        for (String topic:topics
             ) {
            mapMutQueue.put(topic, multQueue);
        }
    }

    /**
     * 初始化发布连接中心
     * @param urls
     */
    public synchronized void initPusblisherProxy(String[] urls)
    {
         this.isBound =true;
         publisher.connect(urls);
    }



    /**
     * 测试发布地址是否成功
     */
    private void  testPusblisaddress(String url,Publisher thepublisher)
    {
        url=url.trim().toLowerCase();
        if(!url.startsWith("tcp://"))
        {
            url="tcp://"+url;
        }
        if(!mapBindUrl.containsKey(url)){
            mapBindUrl.put(url,System.currentTimeMillis());
        }
        else {
            return;//正在测试
        }
        Subscriber tmp=new Subscriber();
        String finalUrl = url;
        tmp.setZmqLog((p)->{
           // System.out.println(p);
        });
        tmp.connect(url);
        tmp.subscriber("innertest",(t, d)->{
           if(t.equals("innertest")||new String(d).equals("innertest"))
           {
               //成功接收以后释放
               mapBindUrl.remove(finalUrl);
               tmp.unSubscriber(t);//清理匿名类
               tmp.disconnect(finalUrl);
               tmp.close();//关闭
               System.out.println("发布地址正常："+finalUrl);
           }
    });
        for (int i = 0; i < 10; i++) {
            ZMQ.sleep(100,TimeUnit.MILLISECONDS);
            thepublisher.publish("innertest", "innertest");
        }

        if(isUnbanded) {
            isUnbanded=false;
            bindLog();
        }
    }

    /**
     * 订阅主题
     * @param topic
     * @param Isubscriber
     */
    public void subscriber(String topic,IBilSubscriber Isubscriber)
    {
        subscriber.subscriber(topic,Isubscriber);
    }

    /**
     *
     * @param topics
     * @param Isubscriber
     */
    public void subscriber(String[] topics,IBilSubscriber Isubscriber)
    {
        for (String topic:topics
             ) {
            subscriber.subscriber(topic,Isubscriber);
        }

    }

    /**
     * 发布数据（需要立即发布，不推荐）
     * @param topic
     * @param data
     */
    public  void publish(String topic,byte[]data)
    {

        publisher.publish(topic,data);
    }

    /**
     * 发布数据（需要立即发布，不推荐）
     * @param topic
     * @param data
     */

    public  void publish(String topic,String data)
    {
        publisher.publish(topic,data);
    }

    /**
     * 发布数据(推荐)
     * @param topic
     * @param data
     */
    public  void publishCache(String topic,String data) throws TcpZmqException {
        byte[] buf = data.getBytes(StandardCharsets.UTF_8);
        this.publishCache(topic,buf);
    }

    /**
     * 发布数据（推荐）
     * @param topic
     * @param data
     */
    public  void publishCache(String topic,byte[] data) throws TcpZmqException {
       if(this.isBound) {
           queue.add(new MsgEntity(topic, data));
           if (isStartCache) {
               msgPack();
               isStartCache = false;
           }
       }
       else
       {
           //zmq虽然允许先发布后初始化，为了安全，必须先初始化，否则缓存过大
           throw  new TcpZmqException("必须先绑定地址");
       }
    }

    /**
     * 多个地址发布
     * @param topic
     * @param data
     * @throws TcpZmqException
     */
    public void  publishMut(String topic,byte[]data) throws TcpZmqException {
      MultQueue msgQueue=  mapMutQueue.get(topic);
      if(msgQueue==null)
      {
          throw new TcpZmqException("没有初始化多个发布主题！");
      }
      else
      {
          msgQueue.add(new MsgEntity(topic,data));
      }
    }

    /**
     * 多个地址发布
     * @param topic
     * @param data
     * @throws TcpZmqException
     */
    public void  publishMut(String topic,String data) throws TcpZmqException {
        byte[]buf=data.getBytes(StandardCharsets.UTF_8);
        this.publishMut(topic,buf);
    }

    /**
     * 缓存发布数据
     */
    private void  msgPack()
    {
        //双重判断，volatile关键字不保障一定成功
        if(reentrantLock.tryLock())
        {
            if(!isStartCache)
            {
                //说明已经启动了
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true)
                    {
                        if(queue.isEmpty())
                        {
                            ZMQ.sleep(100, TimeUnit.MILLISECONDS);
                            continue;
                        }
                        MsgEntity msg=queue.poll();
                        if(msg==null)
                        {
                            continue;
                        }
                        publisher.publish(msg.topic,msg.msg);
                    }
                }
            }).start();
            reentrantLock.unlock();
        }

    }

    /**
     * 发布端测试日志
     */
    private void  bindLog()
    {
        new Thread(() -> {
            checkAddress(mapBindUrl, iZmqLog);
            return;
        }).start();
    }

     void checkAddress(ConcurrentHashMap<String, Long> mapBindUrl, IZmqLog iZmqLog) {
        while (true)
        {
            ZMQ.sleep(1);
            if(mapBindUrl.isEmpty())
            {
                ZMQ.sleep(1);
            }
            for (Map.Entry<String,Long> entry: mapBindUrl.entrySet()
            ) {
                long s=System.currentTimeMillis()-entry.getValue();
                if(s/1000>2&& iZmqLog !=null)
                {
                    iZmqLog.add("发布地址异常，不能接收数据："+entry.getKey());
                }
            }
        }
    }
}
