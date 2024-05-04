package org.cd.zmqnet;

import org.zeromq.ZMQ;

public class ZmqPrxoy {
    public static void  start(int xsubport,int xpubport) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                var context = ZmqCtx.context;
                // 创建前端套接字，并连接到指定地址
                ZMQ.Socket frontend = context.createSocket(zmq.ZMQ.ZMQ_XSUB);
                frontend.bind("tcp://*:"+xsubport);
                // 创建后端套接字，并绑定到指定地址
                ZMQ.Socket backend = context.createSocket(zmq.ZMQ.ZMQ_XPUB);
                backend.bind("tcp://*:" + xpubport);
                try {
                    // 使用 zmq.proxy 进行消息转发
                    ZMQ.proxy(frontend, backend, null);
                } catch (Exception e) {
                    // 处理异常
                    e.printStackTrace();
                } finally {
                    // 关闭套接字和上下文
                    frontend.close();
                    backend.close();
                }

            }

        }).start();
    }
}
