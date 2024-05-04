package org.cd.tcpnet;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class TcpResponse {

    /**
     * 回传iD
     */
    private long clientid ;


    /**
     * 接收的数据
     */
    private  byte[] bytes;

    private Socket mySocket=null;

    private SocketChannel channel=null;
    public TcpResponse(byte[] msg, Socket socket, Long id)
    {
        bytes = msg;
        mySocket = socket;
        clientid=id;
    }

    public TcpResponse(byte[] receiveBytes, SocketChannel client, Long id) {
        bytes = receiveBytes;
        channel = client;
        clientid=id;

    }


    /**
     * 接收的数据
     * @return
     */
    public byte[] GetMsg()
    {
        return bytes;
    }


    /**
     * 回传数据
     */
    public void Send(byte[]data) throws IOException {
        ByteBuffer id=ByteBuffer.allocate(8);
        id.putLong(clientid);
        var by = TcpDelimiter.BuildMessage(data, id);
        if(mySocket!=null)
        {
            mySocket.getOutputStream().write(by);
        }
        if(channel!=null)
        {
            ByteBuffer buffer=ByteBuffer.wrap(by);
            channel.write(buffer);
        }

    }

    /**
     * 单独发送字符串
     * @param msg
     */
    public void Send(String msg) throws IOException {
        byte[]data=msg.getBytes(StandardCharsets.UTF_8);
        this.Send(data);
    }

    /**
     * 主动关闭
     */
    public void Close() throws IOException {
        if(mySocket!=null)
        {
            mySocket.shutdownInput();
            mySocket.shutdownOutput();
            mySocket.close();
        }
        if(channel!=null)
        {
            channel.shutdownInput();
            channel.shutdownOutput();
            channel.close();
        }
    }
}
