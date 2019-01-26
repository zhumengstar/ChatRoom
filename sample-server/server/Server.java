package server;

import com.clink.core.IoContext;
import com.clink.Impl.IoSelectorProvider;
import constants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author:zhumeng
 * @desc: 阻塞->异步   麻烦，不可能
 * 异步->等待   容易，线程锁
 * <p>
 * 　异步当同步，未体现NIO
 **/
public class Server {
    public static void main(String[] args) throws IOException {
        IoContext.setup().ioProvider(new IoSelectorProvider()).start();

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
//            //noinspection ResultOfMethodCallIgnored
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        tcpServer.stop();
        UDPServerProvider.stop();


        IoContext.close();
        System.out.println("Server Stop.");

    }

}
