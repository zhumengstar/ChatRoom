package client;

import client.bean.ServerInfo;
import com.clink.utils.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author:zhumeng
 * @desc:
 **/
public class TCPClient {
    private final Socket socket;
    private final ReadHandler readHandler;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ReadHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit() {
        readHandler.exit();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }

    public void send(String msg) {
        printStream.println(msg);
    }

    public static TCPClient startWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        //超时时间
        socket.setSoTimeout(3000);

        //连接本地端口2000，超时时间3000ms
        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        System.out.println("已发起服务器连接，并进入后续流程～");

        System.out.println("客户端信息：" + socket.getLocalAddress() + "  P:" + socket.getLocalPort());
        System.out.println("服务器端信息：" + socket.getInetAddress() + "  P:" + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();

            return new TCPClient(socket, readHandler);

        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socket);
        }
        return null;
    }

    /**
     * private static void write(Socket client) throws IOException {
     * <p>
     * //构建键盘输入
     * InputStream in = System.in;
     * BufferedReader input = new BufferedReader(new InputStreamReader(in));
     * <p>
     * //得到Socket输出流，并转换为打印流
     * OutputStream outputStream = client.getOutputStream();
     * PrintStream socketPrintStream = new PrintStream(outputStream);
     * <p>
     * do {
     * //键盘读取一行
     * String str = input.readLine();
     * //发送到服务器
     * socketPrintStream.println(str);
     * <p>
     * <p>
     * if ("00bye00".equalsIgnoreCase(str)) {
     * break;
     * }
     * } while (true);
     * <p>
     * //释放资源
     * socketPrintStream.close();}
     **/

    static class ReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }


        @Override
        public void run() {
            super.run();

            try {
                //得到打印流，用于数据输出，服务器回送数据使用
//                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                //得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    String str;
                    try {
                        //客户端拿到数据
                        str = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    if (str == null) {
                        System.out.println("连接已关闭，无法读取数据～");
                        break;
                    }
                    //打印到屏幕
                    System.out.println(str);


                } while (!done);


            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开:" + e.getMessage());
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }

        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }

}
