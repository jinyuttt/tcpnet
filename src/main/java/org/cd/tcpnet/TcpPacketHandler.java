package org.cd.tcpnet;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 处理数据
 */
public class TcpPacketHandler {

    /**
    与 TcpDelimiter一致
     */
    static final byte[] tail = new byte[] { 0xA, 0xF, 0xB, 0xC };

    //缓存
    private byte[] buffer = new byte[10*1024*1024];
    private int receivedLength = 0;
    private Queue<byte[]> packetQueue = new LinkedList<byte[]>();

    /**
     * 尾部校验
     */
    private byte[] buftail = new byte[4];

    /**
     * 每包最大数据量，默认1G，-1时不检查
     */
    public static int MaxSize = 1024 * 1204 * 1024;

    private IPacketReceived packetReceived=null;

    /**
     * 设置数据接收
     * @param iPacketReceived
     */
    public void setPacketReceived(IPacketReceived iPacketReceived)
    {
        this.packetReceived=iPacketReceived;
    }


    /**
     * 转载数据
     * @param data
     * @param length
     */
    public void ReceiveData(byte[] data, int length)
    {
        // 将接收到的数据添加到缓冲区

        System.arraycopy(data,0,buffer,receivedLength,length);
        receivedLength += length;

        while (true)
        {
            // 假设头部为4个字节，包含消息的总长度
            if (receivedLength >= 4)
            {
                ByteBuffer buf=ByteBuffer.wrap(buffer,0,4);
                int messageLength = buf.getInt();
                if (messageLength > buffer.length)
                {
                    //缓存收取不下，扩展缓存
                    if (MaxSize > messageLength||MaxSize<1)
                    {
                        byte[] tmp = new byte[buffer.length];
                        System.arraycopy(buffer,0,tmp,0,tmp.length);
                        buffer = new byte[messageLength + 10240];
                        System.arraycopy(tmp,0,buffer,0,tmp.length);
                    }
                    else
                    {
                        //1.消息大于最大缓存接收，抛弃消息
                        //2.消息接收异常，长度不正确，抛弃
                        ProcessError();
                        continue;
                    }
                }
                if(messageLength<0)
                {
                    //提取的数据异常
                    ProcessError();
                    continue;
                }
                if (receivedLength >= messageLength + 4)
                {
                    // 提取一个完整的消息
                    byte[] fullMessage = new byte[messageLength + 4];//消息长度算了校验字节

                    System.arraycopy(buffer,0,fullMessage,0,messageLength+4);
                    //校验
                    System.arraycopy(buffer,messageLength,buftail,0,4);
                    if (Arrays.equals(tail,buftail))
                    {
                        // 将消息加入队列以供处理
                        packetQueue.add(fullMessage);
                    }
                    else
                    {
                        //从buffer中找到完整数据
                        ProcessError();
                    }

                    // 移动缓冲区剩余的数据到起始位置
                    System.arraycopy(buffer, messageLength + 4, buffer, 0, receivedLength - (messageLength + 4));
                    receivedLength -= messageLength + 4;
                }
                else
                {
                    // 数据不足以构成一个完整的消息，退出循环
                    break;
                }
            }
            else
            {
                // 数据不足以读取头部信息，退出循环
                break;
            }
        }
        if (packetReceived != null)
        {
            // 处理队列中的完整消息
            while (!packetQueue.isEmpty()) {
                //包括长度和尾部
                byte[] packet = packetQueue.poll();
                packetReceived.add(packet);
            }
        }
    }

    /// <summary>
    /// 处理不正常的数据
    /// </summary>
    private void ProcessError()
    {

        try
        {
            int idx = ByteByte(buffer, tail);
            if (idx >= 0)
            {
                //移动buffer，去除校验
                int num = idx + 1;
                System.arraycopy(buffer, idx + 4, buffer, 0, receivedLength - idx-4);
                receivedLength = receivedLength - idx -4;
            }
            else
            {
                //保留最后3字节
                System.arraycopy(buffer, receivedLength - 3, buffer, 0, 3);
                receivedLength = 3;
            }
        }
        catch (Exception ex)
        {

        }
    }

    // KMP算法：得到next数组
    // 思路
    // 1.next数组，按照定义，0和1位置一定分别为-1和0
    public static int[] GetNext(byte[] bytes)
    {
        int[] next = new int[bytes.length];
        int j = 0;
        int k = -1;
        next[j] = k;
        while (j < bytes.length - 1)
        {
            // next[j]是str[j]的最长公共前后缀的长度，也就是说str[0]到str[k]是str[j]的最长公共前后缀，
            if (k == -1 || bytes[j] == bytes[k])// 如果相等，str[k] == str[j]，即找到了str[0]->str[k]和str[j-k]->str[j]，也就是0到k和j-k到j的一个公共前后缀，
            {
                k++;
                next[j + 1] = k;// 也就可以得出j+1的最长公共前后缀，因为此时k=j，而0-k是j的一个公共前后缀，因此，j+1的最长公共前后缀也就是k+1
                // 所以next[j+1]，也就是j+1的最长公共前后缀的长度为k+1
                j++;
            }
            else// 如果不相等，那么就需要进一步寻找更短的最长公共前后缀，
            {
                k = next[k];// 而next[k]则表示str[k]的最长公共前后缀所在的位置，也就是更短的最长公共前后缀
            }
        }
        return next;
    }
    // KMP算法：模式匹配
    public static int ByteByte(byte[] haystack, byte[] needle)
    {
        int len = haystack.length;
        if (len == 1)
        {
            return 0;
        }
        int[] next = GetNext(needle);
        int index = 0;
        int i = 0;
        while (index < next.length && i < len)
        {
            if (index == -1 || haystack[i] == needle[index])
            {
                i++;
                index++;
            }
            else
            {
                index = next[index];
            }
        }
        if (i >= len && index < next.length)
        {
            return -1;
        }
        return i - needle.length;
    }
}
