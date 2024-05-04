package org.cd;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 线程池
 */
public  class ThreadManager {

    static ExecutorService executorService= Executors.newCachedThreadPool();

    /**
     * 对象
     * @return
     */
   public static  ExecutorService getExecutorService()
   {
       return  executorService;
   }

   public static void  sleep(long seconds)
   {
       try {
           Thread.sleep(seconds*1000);
       } catch (InterruptedException e) {
           throw new RuntimeException(e);
       }
   }
    public static void   sleep(long mill, TimeUnit timeUnit)
    {
        try {
           Thread.sleep(timeUnit.toMillis(mill));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
