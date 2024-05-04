package org.cd.zmqnet;

import org.zeromq.ZContext;

/**
 * 同一个上下文
 */
  class ZmqCtx {

    /**
     * 上下文
     */
    public static ZContext context = new ZContext();

   static {
       context.setLinger(100);//100ms
       context.setRcvHWM(100000);
       context.setSndHWM(100000);
   }

}
