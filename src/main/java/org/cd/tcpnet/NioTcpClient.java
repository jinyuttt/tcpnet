package org.cd.tcpnet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class NioTcpClient {

    SocketChannel socketChannel=null;

    private final ConcurrentHashMap<Long,byte[]> map=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long,ITcpCallBack> mapTcp=new ConcurrentHashMap<>();
    private int bufferSize = 1024*1024 ;
    public synchronized void connect(String ip,int port) throws IOException {
         socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE)
                .setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
        socketChannel.connect(new InetSocketAddress(ip, port));
        while (!socketChannel.finishConnect()) {
            // 可以添加等待连接完成的处理逻辑
            System.out.println("连接服务器中...");
        }
        receiveMessage();
       System.out.println("连接服务器成功");
    }

    /**
     * 发送数据
     * @param data
     * @return
     * @throws IOException
     */
    public long send(byte[]data) throws IOException {
        ByteBuffer id=ByteBuffer.allocate(8);
        id.putLong(-1);
        if(socketChannel.isConnected())
        {
            byte[]buf= TcpDelimiter.BuildMessage(data,id);
            ByteBuffer buffer = ByteBuffer.wrap(buf);
            socketChannel.write(buffer);
        }
        id.flip();
     return  id.getLong();
    }

    /**
     * 发送数据
     * @param msg
     * @throws IOException
     */
    public long send(String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        return this.send(bytes);
    }

    /**
     * 发送并返回
     * @param data
     * @param timeout
     * @return
     * @throws IOException
     */
    public byte[] sendAndReply(byte[]data, long timeout) throws IOException {
        byte[] buf = null ;
        if (socketChannel.isConnected())
        {
            ByteBuffer  id = ByteBuffer.allocate(8);
            id.putLong(-1);
            id.flip();
            int num = 1;
            byte[] bytes = TcpDelimiter.BuildMessage(data,  id);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            socketChannel.write(buffer);
            id.flip();
            while (true)
            {
                buf= map.get(id.getLong());
                if(buf!=null)
                {
                    break;
                }
                num++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (num * 100> timeout)
                {
                    break;
                }
            }
        }
        return buf;
    }

    /**
     * 发送并且返回
     * @param msg
     * @param timeout
     * @return
     * @throws IOException
     */
    public  byte[]sendAndReply(String msg,long timeout) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        return this.sendAndReply(bytes, timeout);
    }

    /**
     * 设置数据回调读取
     * @param id
     * @param callBack
     */
    public  void  setCallBack(long id,ITcpCallBack callBack)
    {
        mapTcp.put(id,callBack);
    }


    public void close() throws IOException {
        socketChannel.shutdownInput();
        socketChannel.shutdownOutput();
        socketChannel.close();
    }
    /**
     * 接收
     */
    private void receiveMessage()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int bytesLen=-1;
                byte[] bytes = new byte[bufferSize];
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                ByteBuffer id = ByteBuffer.allocate(8);
                id.putLong(-1);
                TcpPacketHandler tcpPacket = new TcpPacketHandler();
                tcpPacket.setPacketReceived(p->
                {
                    byte[] msg = TcpDelimiter.GetMessage(p,  id);
                    id.flip();
                    long msgid= id.getLong();
                    if(mapTcp.containsKey(msgid))
                    {
                        mapTcp.get(msgid).read(msg);
                    }
                    else
                    {
                        map.put(msgid,msg);
                    }

                });
                while (true)
                {
                    try {
                        bytesLen = socketChannel.read(buffer);
                        if(bytesLen!=-1)
                        {
                            tcpPacket.ReceiveData(bytes, bytesLen);
                        }
                        else
                        {
                            socketChannel.shutdownOutput();
                            socketChannel.shutdownInput();
                            socketChannel.close();
                        }
                        buffer.clear();
                        if (!socketChannel.isConnected())
                        {
                            break;
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
                tcpPacket = null;
            }
        }).start();
    }

}
