package server2;

import com.clinke.utils.CloseUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author:zhumeng
 * @desc:
 **/
public class TCPServer1 implements ClientHandler1.ClientHandlerCallback {
    private final int port;
    //nio����
    private ClientListener listener;

    //�̰߳�ȫ����֤ɾ��ʱ����ӣ�������ȫ
    private List<ClientHandler1> clientHandles = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService forwardingThreadPollExecutor;

    //ѡ����
    private Selector selector;
    //
    private ServerSocketChannel server;

    public TCPServer1(int port) {

        this.port = port;
        //ת���̳߳�
        this.forwardingThreadPollExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {


            selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            //����Ϊ������
            server.configureBlocking(false);
            //�󶨱��ض˿�
            server.socket().bind(new InetSocketAddress(port));
            this.server = server;
            //ע��ͻ������ӵ������
            server.register(selector, SelectionKey.OP_ACCEPT);


            System.out.println("��������Ϣ��" + server.getLocalAddress().toString());

            //�����ͻ��˼���
            ClientListener listener = this.listener = new ClientListener();
            //accept
            listener.start();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    public void stop() {
        if (listener != null) {
            listener.exit();
        }

        CloseUtils.close(server);
        CloseUtils.close(selector);

        //ͬ�������̰߳�ȫ
        synchronized (TCPServer1.this) {
            for (ClientHandler1 clientHandle : clientHandles) {
                clientHandle.exit();
            }
            clientHandles.clear();
        }
        forwardingThreadPollExecutor.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler1 clientHandle : clientHandles) {
            clientHandle.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler1 handler) {
        clientHandles.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler1 handler, final String msg) {
        //��ӡ����Ļ��
        System.out.println("Received-" + handler.getClientInfo() + ":" + msg);
        //�첽�ύת������
        forwardingThreadPollExecutor.execute(() -> {
            for (ClientHandler1 clientHandler : clientHandles) {
                if (clientHandler.equals(handler)) {
                    //�����Լ�
                    continue;
                }
                //��������
                clientHandler.send(msg);
            }
        });


    }

    private class ClientListener extends Thread {
        private boolean done = false;


        @Override
        public void run() {
            super.run();


            Selector selector = TCPServer1.this.selector;


            System.out.println("������׼��������");
            //�ȴ��ͻ�������
            do {

                try {
                    //��Զ����select
                    //select��ʾ��ǰ��û���¼������������ж���
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;

                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        //��鵱ǰkey��״̬�Ƿ�ʱ���ǹ�ע�Ŀͻ��˵���״̬
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            //�õ��������Ŀͻ������ӣ����Խ��пͻ����첽����
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            try {
                                //�ͻ��˹����첽�߳�
                                ClientHandler1 clientHandle = new ClientHandler1(socketChannel, TCPServer1.this);
                                //��ȡ���ݲ���ӡ
                                clientHandle.readToPrint();
                                synchronized (TCPServer1.this) {
                                    clientHandles.add(clientHandle);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("�ͻ��������쳣" + e.getMessage());
                            }
                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            while (!done);
            System.out.println("�������ѹرա�");
        }

        void exit() {
            done = true;
            //���ѵ�ǰ������
            selector.wakeup();
        }

    }

    //�ͻ�����Ϣ����
//    private static class ClientHandle extends Thread {
//        private Socket socket;
//        private boolean flag = true;
//
//        ClientHandle(Socket socket) {
//            this.socket = socket;
//        }
//
//        @Override
//        public void run() {
//            super.run();
//            System.out.println("�¿ͻ������ӣ�" + socket.getInetAddress() + "P:" + socket.getLocalPort());
//
//            try {
//                //�õ���ӡ�������������������������������ʹ��
//                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
//                //�õ������������ڽ�������
//                BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//                do {
//                    //�ͻ����õ�����
//                    String str = socketInput.readLine();
//                    if ("bye".equalsIgnoreCase(str)) {
//                        flag = false;
//                        //����
//                        System.out.println("bye");
//                        socketOutput.println("bye");
//
//                    } else {
//                        //��ӡ����Ļ�����������ݳ���
//                        if (str != null) {
//                            System.out.println(str);
//                        }
//
//                        //���ؿͻ���
//                        socketOutput.println("���ͳ���:" + str.length());
//                    }
//                } while (flag);
//
//                socketInput.close();
//                socketOutput.close();
//
//            } catch (Exception e) {
//                System.out.println("�����쳣�Ͽ���");
//            } finally {
//                //���ӹر�
//                try {
//                    socket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("�ͻ������˳�" + socket.getInetAddress() + "P:" + socket.getPort());
//        }
//
//        public void exit(ClientHandle clientHandle) {
//
//        }
//
//        public void send(String str) {
//
//        }
//    }
}
