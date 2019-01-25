package client;

import client.bean.ServerInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author:zhumeng
 * @desc:
 **/
public class ClientTest {
    private static boolean done;

    public static void main(String[] args) throws IOException {

        ServerInfo info = UDPClientSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info == null) {
            return;
        }

        //��ǰ��������
        int size = 0;
        List<TCPClient> tcpClients = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(info);
                if (tcpClient == null) {
                    System.out.println("�����쳣��");
                    continue;
                }
                tcpClients.add(tcpClient);
                size++;
                System.out.println("���ӳɹ���" + size);
            } catch (IOException e) {
                System.out.println("�����쳣��");
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.in.read();

        Runnable runnable = () -> {
            while (!done) {
                for (TCPClient tcpClient : tcpClients) {
                    tcpClient.send("hello");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread thread = new Thread(runnable);

        thread.start();
        System.in.read();
        done = true;
        //�ȴ��߳����
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //�ͻ��˽�������
        for (TCPClient tcpClient : tcpClients) {
            tcpClient.exit();
        }
    }

}
