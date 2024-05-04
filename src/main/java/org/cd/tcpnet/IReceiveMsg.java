package org.cd.tcpnet;

/**
 * 服务端接收的消息
 */
public interface IReceiveMsg {
    void OnMessage(TcpResponse tcpResponse);
}
