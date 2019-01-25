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
        //��ʱʱ��
        socket.setSoTimeout(3000);

        //���ӱ��ض˿�2000����ʱʱ��3000ms
        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        System.out.println("�ѷ�����������ӣ�������������̡�");

        System.out.println("�ͻ�����Ϣ��" + socket.getLocalAddress() + "  P:" + socket.getLocalPort());
        System.out.println("����������Ϣ��" + socket.getInetAddress() + "  P:" + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();

            return new TCPClient(socket, readHandler);

        } catch (Exception e) {
            System.out.println("�����쳣");
            CloseUtils.close(socket);
        }
        return null;
    }

    /**
     * private static void write(Socket client) throws IOException {
     * <p>
     * //������������
     * InputStream in = System.in;
     * BufferedReader input = new BufferedReader(new InputStreamReader(in));
     * <p>
     * //�õ�Socket���������ת��Ϊ��ӡ��
     * OutputStream outputStream = client.getOutputStream();
     * PrintStream socketPrintStream = new PrintStream(outputStream);
     * <p>
     * do {
     * //���̶�ȡһ��
     * String str = input.readLine();
     * //���͵�������
     * socketPrintStream.println(str);
     * <p>
     * <p>
     * if ("00bye00".equalsIgnoreCase(str)) {
     * break;
     * }
     * } while (true);
     * <p>
     * //�ͷ���Դ
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
                //�õ���ӡ�������������������������������ʹ��
//                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                //�õ������������ڽ�������
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    String str;
                    try {
                        //�ͻ����õ�����
                        str = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    if (str == null) {
                        System.out.println("�����ѹرգ��޷���ȡ���ݡ�");
                        break;
                    }
                    //��ӡ����Ļ
                    System.out.println(str);


                } while (!done);


            } catch (Exception e) {
                if (!done) {
                    System.out.println("�����쳣�Ͽ�:" + e.getMessage());
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
