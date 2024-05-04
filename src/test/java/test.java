import org.cd.ThreadManager;
import org.cd.zmqnet.*;
import org.zeromq.ZMQ;

public class test {
    public  static  void  main(String[] arg)
    {

       // testzmq();
       // testZmqFactory();
       //  testZmqThreadFactory();
       // testMultzmq();
      //  testProxy();
      // var lst= Util.getAllIpAddress();
        testStatic();
    }


    /**
     * zmq基础测试
     */
    private  static  void  testzmq()
    {
        Publisher publisher=new Publisher();
        publisher.bind(7777);
        publisher.publish("A","AA");
        Subscriber subscriber=new Subscriber();
        subscriber.connect("tcp://127.0.0.1:7777");
        subscriber.subscriber("A",(t,d)->{
            System.out.println(t+","+new String(d));
        });
        //subscriber.connect("tcp://127.0.0.1:7777");测试表明连接和接收没有先后顺序

        while (true)
        {
            publisher.publish("A","BBB");
            ZMQ.sleep(1);
        }
    }


    /**
     * 单例测试
     */
    private static void  testZmqFactory()
    {
        ZmqInstance.getInstance().setZmqLog((p)->{
            System.out.println(p);
        });
        ZmqInstance.getInstance().initPusblisherBind(5555);
        ZmqInstance.getInstance().initSubscriber(new String[]{"127.0.0.1:5555"});
        ZmqInstance.getInstance().subscriber("A",(t, d)->{
            System.out.println("topic:"+t+","+new String(d));
        });

        int num=1;
        while (true)
        {

           for (int i = 0; i < 10000; i++) {
                ZmqInstance.getInstance().publish("A",String.valueOf(num)+"_"+String.valueOf(i));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            num++;
        }
    }

    /**
     * 线程测试
     */
    private static void  testZmqThreadFactory()
    {
        ZmqInstance.getInstance().setZmqLog((p)->{
            System.out.println(p);
        });
        ZmqInstance.getInstance().initPusblisherBind(5555);
        ZmqInstance.getInstance().subscriber("A",(t, d)->{
            System.out.println("topic:"+t+","+new String(d));
        });
        ZmqInstance.getInstance().initSubscriber(new String[]{"127.0.0.1:5555"});
        int num=1;
        while (true)
        {
            int finalNum = num;
            for (int i = 0; i <3 ; i++) {
                ThreadManager.getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {

                        for (int i = 0; i < 10000; i++) {
                            try {
                                ZmqInstance.getInstance().publishCache("A",String.valueOf(finalNum)+"_"+String.valueOf(i));
                            } catch (TcpZmqException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
            }


            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            num+=3;
        }
    }


    /**
     * 多个发布地址测试
     */
    private  static  void  testMultzmq()
    {
        ZmqInstance.getInstance().initSubscriber(new String[]{"127.0.0.1:6666","127.0.0.1:6667"});
        ZmqInstance.getInstance().subscriber(new String[]{"A","B","C"},(t, d)->{
            System.out.println(t+","+new String(d));
        });
        ZmqInstance.getInstance().setZmqLog((p)->{
          //  System.out.println(p);
        });
        try {
            ZmqInstance.getInstance().initMutPublisher(new String[]{"A","B"},"tcp://*:6666");
           // ZmqFactory.getInstance().initMutPublisher(new String[]{"C","D"},"tcp://*:6667");
        } catch (TcpZmqException e) {
            throw new RuntimeException(e);
        }
        while (true)
        {
            ZMQ.sleep(1);
            try {
                ZmqInstance.getInstance().publishMut("A","AAAAA");
                ZmqInstance.getInstance().publishMut("B","BBBB");
              //  ZmqFactory.getInstance().publishMut("C","CCCCC");
            } catch (TcpZmqException e) {
                throw new RuntimeException(e);
            }

        }
    }


    /**
     * 代理测试
     */
    private  static  void  testProxy()
    {
        ZmqPrxoy.start(7777,8888);
        Publisher  publisher=new Publisher();
        publisher.connect("127.0.0.1:7777");
        Subscriber subscriber=new Subscriber();
        subscriber.connect("127.0.0.1:8888");
        subscriber.subscriber("A",(t,d)->{
            System.out.println(t+","+new String(d));
        });
        while (true)
        {
            ZMQ.sleep(1);
            publisher.publish("A","FFFFF");
        }
    }

    private  static  void  testStatic()
    {
        ZmqFct.setZmqLog((p)->{
            System.out.println(p);
        });
        ZmqFct.initPusblisherBind(5555);
        ZmqFct.initSubscriber(new String[]{"127.0.0.1:5555"});
        ZmqFct.subscriber("A",(t, d)->{
            //System.out.println("topic:"+t+","+new String(d));
        });

        int num=1;
        while (true)
        {

            for (int i = 0; i < 10000; i++) {
                ZmqFct.publish("A",String.valueOf(num)+"_"+String.valueOf(i));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            num++;
        }
    }
}
