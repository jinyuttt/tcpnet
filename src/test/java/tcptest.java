import org.cd.tcpnet.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class tcptest {
    public static void main(String[]v)
    {
      //  testKmp();
       // test();
       // testChanel();
        //testCalback();
        testbackchanel();
    }
    static void test()
    {
        TcpServer server=new TcpServer(4444);
        server.setiReceiveMsg((p)->{
            try {
                p.Send("hi");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println(new String(p.GetMsg()));

                }
        );
        server.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        TcpClient client=new TcpClient();
        try {
            client.connect("127.0.0.1",4444);
            while (true)
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (int i = 0; i < 1000; i++) {
                    client.send("ttttt"+i);
                    byte[]bytes= client.sendAndReply("hrrrr"+i,2000);
                    if(bytes!=null)
                        System.out.println(new String(bytes));
                }
                System.out.println("***********************");

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    static  void  testCalback()
    {
        TcpServer server=new TcpServer(4444);
        server.setiReceiveMsg((p)->{
                    try {
                        String str=new String(p.GetMsg());
                        if(str.equals("hi"))
                        {
                            System.out.println("收到客户端");
                            while (true)
                            {
                                for (int i=0;i<1000;i++)
                                {
                                    p.Send(i+","+System.currentTimeMillis()+""+"kkkkk");
                                }
                               Thread.sleep(1000);
                               break;
                            }
                            p.Close();
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }


                }
        );
        server.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        TcpClient client=new TcpClient();
        try {
            client.connect("127.0.0.1",4444);
            long id= client.send("hi");

                client.setCallBack(id,(p)->{
                    System.out.println(new String(p));
                });
                System.out.println("***********************");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void  testKmp()
    {
        byte[] bytes=new byte[50];
        byte[] ty=new byte[]{0xA,0xB,0xF,0xD};
        ByteBuffer buffer=ByteBuffer.wrap(bytes);
        buffer.put(new byte[]{5,8,1,4,7,9,0,9,1,0});
        byte[]d="ffttuiioffhjjkleegj".getBytes(StandardCharsets.UTF_8);
        buffer.put(d);
        buffer.put(ty);
        buffer.put(new byte[]{22,56,89,11,127,14,101,67,8,9,79});
       int r= TcpPacketHandler.ByteByte(bytes,ty);
       System.out.println(r);
    }


    static  void  testChanel()
    {
        NioTcpServer server=new NioTcpServer(7812);
        server.setiReceiveMsg((p)->{
           System.out.println(new String(p.GetMsg()));
            try {
                p.Send("hi!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        server.start();
        NioTcpClient client=new NioTcpClient();
        try {
            client.connect("127.0.0.1",7812);
            while (true)
            {
                Thread.sleep(1000);
                client.send("tttt");
               byte[]t= client.sendAndReply("hrrrr",2000);
               if(t!=null)
               {
                   System.out.println(new String(t));
               }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    static  void  testbackchanel()
    {
        NioTcpServer server=new NioTcpServer(7812);
        server.setiReceiveMsg((p)->{

            try {
                String str=new String(p.GetMsg());
                if(str.equals("hi"))
                {
                    System.out.println("收到客户端");
                    while (true)
                    {
                        for (int i=0;i<1000;i++)
                        {
                            p.Send(i+","+System.currentTimeMillis()+""+"kkkkk");
                        }
                        Thread.sleep(1000);
                        break;
                    }
                    p.Close();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        server.start();
        NioTcpClient client=new NioTcpClient();
        try {
            client.connect("127.0.0.1",7812);
           long id= client.send("hi");
            client.setCallBack(id, (p)->
            {
                System.out.println(new String(p));
            });
            System.out.println("***********************");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
