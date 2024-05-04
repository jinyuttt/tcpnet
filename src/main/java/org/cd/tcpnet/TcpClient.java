package org.cd.tcpnet;
import org.cd.ThreadManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * tcp客户端
 */
public class TcpClient {
    private final Socket socket=new Socket();
    private final ConcurrentHashMap<Long,byte[]> map=new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long,ITcpCallBack> mapTcp=new ConcurrentHashMap<>();
    private int bufferSize = 1024*1024 ;



    /**
     * 设置接收池大小，默认1M
     * @param size
     */
    public void setBufferSize(int size)
    {
        this.bufferSize=size;
    }

    /**
     * 连接
     * @param ip
     * @param port
     * @throws IOException
     */
    public synchronized void connect(String ip,int port) throws IOException {
        SocketAddress socketAddress =  new InetSocketAddress(ip, port);
        socket.connect(socketAddress);
        receiveMessage();
    }

    /**
     * 发送数据
     * @param data
     * @return  回话ID
     * @throws IOException
     */
    public long send(byte[]data) throws IOException {
      //
        ByteBuffer id=ByteBuffer.allocate(8);
        id.putLong(-1L);
        if(socket.isConnected()) {
            byte[] buf = TcpDelimiter.BuildMessage(data, id);
            socket.getOutputStream().write(buf);
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
        byte[]bytes=msg.getBytes(StandardCharsets.UTF_8);
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
        if (socket.isConnected())
        {
            ByteBuffer  id = ByteBuffer.allocate(8);
            int num = 1;
            id.putLong(-1L);
            id.flip();
            byte[] bytes = TcpDelimiter.BuildMessage(data,  id);
            socket.getOutputStream().write(bytes);
            while (true)
            {
                id.flip();
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
     * 回传数据,必须在send方法之前
     * @param id
     * @param callBack
     */
    public void setCallBack(long id,ITcpCallBack callBack) {
        mapTcp.put(id, callBack);
    }


    public  void  close() throws IOException {
        socket.close();
        map.clear();
        mapTcp.clear();
    }

    /**
     * 接收的数据量
     * @return
     */
    public int getCount()
    {
        return  map.size();
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
                byte[] buffer = new byte[bufferSize];

                TcpPacketHandler tcpPacket = new TcpPacketHandler();
                tcpPacket.setPacketReceived(p->
                {
                    ByteBuffer id = ByteBuffer.allocate(8);
                    id.putLong(-1);
                    byte[] msg = TcpDelimiter.GetMessage(p,  id);
                    id.flip();
                    long msgid=id.getLong();
                    if(mapTcp.containsKey(msgid))
                    {

                        mapTcp.get(msgid).read(msg);
                    }
                    else {
                        map.put(msgid, msg);
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                while (true)
                {
                    try {
                        bytesLen = socket.getInputStream().read(buffer);
                        if(bytesLen!=-1) {
                            tcpPacket.ReceiveData(buffer, bytesLen);
                        }
                        else
                        {
                            socket.shutdownInput();
                        }
                        if (!socket.isConnected())
                        {
                            break;
                        }
                    } catch (IOException e) {
                       break;
                    }
                }
                tcpPacket = null;
                System.out.println("客户端退出读取");
            }
        }).start();
    }
}
