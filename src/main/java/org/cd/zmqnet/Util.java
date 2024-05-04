package org.cd.zmqnet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Util {

    /**
     * 获取IP
     * @return
     */
    public static List<String> getAllIpAddress() {
        List<String> lst=new ArrayList<>();
        try {

            //get all network interface
            Enumeration<NetworkInterface> allNetworkInterfaces =
                    NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface = null;

            //check if there are more than one network interface
            while (allNetworkInterfaces.hasMoreElements()) {
                //get next network interface
                networkInterface = allNetworkInterfaces.nextElement();
                //output interface's name
                System.out.println(networkInterface.getDisplayName());

                Enumeration<InetAddress> allInetAddress =
                        networkInterface.getInetAddresses();

                InetAddress ipAddress = null;

                while (allInetAddress.hasMoreElements()) {
                    //get next ip address
                    ipAddress = allInetAddress.nextElement();
                    if (ipAddress != null && ipAddress instanceof Inet4Address) {
                        lst.add(ipAddress.getHostAddress());
//                        System.out.println("ip address: " +
//                                ipAddress.getHostAddress());
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        return  lst;
    }
}
