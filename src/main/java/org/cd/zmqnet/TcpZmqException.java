package org.cd.zmqnet;


/**
 * 异常
 */
public class TcpZmqException extends  Exception{
    public  TcpZmqException(String errormsg)
    {
        super(errormsg);
    }
}
