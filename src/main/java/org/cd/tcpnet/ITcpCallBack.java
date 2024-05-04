package org.cd.tcpnet;


/**
 * 客户端接收消息
 */
public interface ITcpCallBack {
      void read(byte[]data);
}
