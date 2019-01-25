package client;

import client.bean.ServerInfo;

/**
 * @author:zhumeng
 * @desc:
 **/
public class Client {
    public static void main(String[] args) {
        ServerInfo info = UDPClientSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            try {
                TCPClient.linkWith(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

