package org.cd.tcpnet;

/**
 * 接收解析的数据
 */
 interface IPacketReceived {
    void add(byte[]data);
}
