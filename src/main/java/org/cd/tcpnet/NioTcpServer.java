package org.cd.tcpnet;

import org.cd.ThreadManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioTcpServer {

    private  int port;
    ServerSocketChannel serverSocket =null;

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

    public  void setiReceiveMsg(IReceiveMsg receiveMsg)
    {
       this.iReceiveMsg=receiveMsg;
    }

    public  NioTcpServer(int port)
    {
        this.port=port;
    }



    public boolean  start()
    {
        try {
            serverSocket = ServerSocketChannel.open();
            Selector selector = Selector.open();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            listenClientConnect(selector);
            return  true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return  false;
    }

    /**
     * 开始接收客户端数据
     */
    private void listenClientConnect(Selector selector)
    {
        Thread t=new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        selector.select();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if (key.isAcceptable()) {
                            try {
                                SocketChannel client = serverSocket.accept();
                                client.configureBlocking(false);
                                client.register(selector, SelectionKey.OP_READ);                             System.out.println("连接成功！");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            TcpPacketHandler tcpPacket = new TcpPacketHandler();
                            tcpPacket.setPacketReceived((p) -> {

                                ByteBuffer id = ByteBuffer.allocate(8);
                                id.putLong(-1);
                                byte[] receiveBytes = null;
                                receiveBytes = TcpDelimiter.GetMessage(p, id);
                                if (receiveBytes != null) {
                                    id.flip();
                                    TcpResponse tcpResponse = new TcpResponse(receiveBytes, client, id.getLong());
                                    if (iReceiveMsg != null) {
                                        iReceiveMsg.OnMessage(tcpResponse);
                                    }
                                }
                            });
                            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                            while (true) {
                                try {
                                    if (!(client.read(buffer) > 0||!client.isConnected())) break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    try {
                                        client.close();
                                    } catch (IOException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }

                                // 处理接收到的数据
                                tcpPacket.ReceiveData(buffer.array(), buffer.position());
                                buffer.clear();
                            }
                        } else if (key.isWritable()) {
                           System.out.println("ddddd");
                        }

                    }
                }
            }
        });
        t.setName("ServerSocket-"+port);
        t.setDaemon(true);
        t.start();
        System.out.println("退出");
    }
}
