package org.cd.tcpnet;

import java.nio.ByteBuffer;


/**
 * 分取数据
 */
 class TcpDelimiter {
    static final byte[] tail = new byte[] {0xA,0xF,0xB,0xC};


    static final SnowflakeIdGenerator snowflake = new SnowflakeIdGenerator(1,1);


    /**
     * 组装数据
     * @param message
     * @return
     */
    public static byte[] BuildMessage(byte[] message, ByteBuffer bufid)
    {

        byte[] result = new byte[message.length + 4 + 8+4];//4个字节长度+8字节id+4自己尾部；
        ByteBuffer buffer=ByteBuffer.wrap(result);
        buffer.putInt(message.length+12);//长度不算自己但是要算尾部
        bufid.flip();
        long id=bufid.getLong();
        if(id==-1) {
            id = snowflake.nextId();
            bufid.clear();
            bufid.putLong(id);
        }
        buffer.putLong(id);
        buffer.put(message);
        buffer.put(tail);
        return result;
    }


    /**
     * 解析数据
     * @param message
     * @return
     */
    public static byte[] GetMessage(byte[] message,  ByteBuffer bufid)
    {
        ByteBuffer buffer=ByteBuffer.wrap(message);
        buffer.getInt();//去头 长度4
        bufid.clear();
       long id=buffer.getLong();//8
        bufid.putLong(id);
        byte[] data=new byte[message.length-16];//去尾 校验4字节
        buffer.get(data);
        return data;
    }

}
