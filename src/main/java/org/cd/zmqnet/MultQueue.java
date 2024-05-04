package org.cd.zmqnet;

import org.zeromq.ZMQ;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * 缓存发布（多个发布时）
 */
 class MultQueue {
    private ConcurrentLinkedQueue<MsgEntity> queue=new ConcurrentLinkedQueue<>();

    private Publisher publisher=null;

    public MultQueue(Publisher publisher)
    {
        this.publisher=publisher;
        msgPack();
    }

    public  void  add(MsgEntity msg)
    {
        queue.add(msg);
    }
    /**
     * 缓存发布数据
     */
    private void  msgPack()
    {

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


    }
}
