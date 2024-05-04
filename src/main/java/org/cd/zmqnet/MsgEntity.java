package org.cd.zmqnet;

/**
 * 缓存消息
 */
 class MsgEntity {
    public String topic;
    public  byte[] msg;

    public MsgEntity(String t,byte[]ct)
    {
        this.topic=t;
        this.msg=ct;
    }
}
