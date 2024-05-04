package org.cd.tcpnet;

import org.cd.ThreadManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Tcp服务端
 */
public class TcpServer {

    ServerSocket socket=null;

    private  int port=0;

    private int bufferSize=1024*1024;

    private IReceiveMsg iReceiveMsg=null;

    /**
     * 设置接收缓存,默认1M
     * @param size
     */
    public void setBufferSize(int size)
    {
        this.bufferSize=size;
    }

    /**
     * 接收数据
     * @param receiveMsg
     */
    public void setiReceiveMsg(IReceiveMsg receiveMsg)
    {
        this.iReceiveMsg=receiveMsg;
    }



    public  TcpServer(int port)
    {
        this.port=port;
    }

    /**
     * 开始
     * @return
     */
    public boolean start() {
        try {
            socket = new ServerSocket(port);
            listenClientConnect();
            return true;
        } catch (IOException e) {
            System.out.println(e);
        }
        return false;
    }

    /**
     * 开始接收客户端数据
     */
    private void listenClientConnect()
    {
        Thread t=new Thread(new Runnable() {
            @Override
            public void run() {
                while (!socket.isClosed())
                {
                    try {
                        Socket client=socket.accept();
                        System.out.println("接收到客户端");
                        ThreadManager.getExecutorService().execute(new Runnable() {
                            @Override
                            public void run() {
                                receiveBufferMessage(client);
                            }
                        });

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        t.setName("ServerSocket-"+port);
        t.setDaemon(true);
        t.start();
    }

    /**
     * 客户端数据处理
     * @param clientSocket
     */
    private  void receiveBufferMessage(Socket clientSocket)
    {
        Socket myClientSocket = clientSocket;
        TcpPacketHandler tcpPacket = new TcpPacketHandler();
        tcpPacket.setPacketReceived((p) -> {
        ByteBuffer id = ByteBuffer.allocate(8);
        id.putLong(-1);
        byte[] receiveBytes = null;
        receiveBytes = TcpDelimiter.GetMessage(p,   id);
        if (receiveBytes != null)
        {
            id.flip();
            TcpResponse tcpResponse = new TcpResponse(receiveBytes, myClientSocket,id.getLong());
            if (iReceiveMsg != null)
            {
                //一个线程处理一个客户端，这里不用线程，保障数据顺序，性能处理交给业务层。
                //也可以加缓存集合，但是在传输层大量缓存数据也不合适。
                iReceiveMsg.OnMessage(tcpResponse);
            }
        }});

        byte[] buffer = new byte[bufferSize];
        while (myClientSocket.isConnected())
        {
            try
            {
                //通过clientSocket接收数据
                int receiveNumber =  myClientSocket.getInputStream().read(buffer);
                tcpPacket.ReceiveData(buffer, receiveNumber);
            }
            catch (Exception ex)
            {
                try {
                    myClientSocket.close();//关闭Socket并释放资源
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                break;
            }
        }
        tcpPacket = null;
    }

}
