package server;

import constants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author:zhumeng
 * @desc:
 **/
public class Server {
    public static void main(String[] args) throws IOException {
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("Start TCP Server failed.");
            return;
        }

        UDPServerProvider.start(TCPConstants.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();

            tcpServer.broadcast(str);

        } while (!"00bye00".equalsIgnoreCase(str));


//        UDPServerProvider.start(TCPConstants.PORT_SERVER);
//
//        try {
//            //noinspection ResultOfMethedCallIgnored
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        tcpServer.stop();
        UDPServerProvider.stop();
        System.out.println("Server Stop.");

    }

}
