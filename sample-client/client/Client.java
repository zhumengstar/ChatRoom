package client;

import client.bean.ServerInfo;

import java.io.*;

/**
 * @author:zhumeng
 * @desc:
 **/
public class Client {
    public static void main(String[] args) {
        ServerInfo info = UDPClientSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(info);
                if (tcpClient == null) {
                    return;
                }
                write(tcpClient);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
    }

    private static void write(TCPClient client) throws IOException {
        //构建键盘输入
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));


        do {
            //键盘读取一行
            String str = input.readLine();
            //发送到服务器
            client.send(str);

            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }
        } while (true);
    }
}

