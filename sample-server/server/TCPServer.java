package server;

import com.clink.utils.CloseUtils;
import server.handle.ClientHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
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
public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    //nio����
    private ClientListener listener;

    //�̰߳�ȫ����֤ɾ��ʱ����ӣ�������ȫ
    private List<ClientHandler> clientHandles = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService forwardingThreadPollExecutor;

    //ѡ����
    private Selector selector;
    //
    private ServerSocketChannel server;

    public TCPServer(int port) {

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
            //ע��ͻ������ӵ������
            server.register(selector, SelectionKey.OP_ACCEPT);

            this.server = server;


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
        synchronized (TCPServer.this) {
            for (ClientHandler clientHandle : clientHandles) {
                clientHandle.exit();
            }
            clientHandles.clear();
        }
        forwardingThreadPollExecutor.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler clientHandle : clientHandles) {
            clientHandle.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandles.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        //��ӡ����Ļ��
        System.out.println("Received-" + handler.getClientInfo() + ":" + msg);
        //�첽�ύת������
        forwardingThreadPollExecutor.execute(() -> {
            for (ClientHandler clientHandler : clientHandles) {
//                if (clientHandler.equals(handler)) {
//                    //�����Լ�
//                    continue;
//                }
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


            Selector selector = TCPServer.this.selector;


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
                                ClientHandler clientHandler = new ClientHandler(socketChannel, TCPServer.this);
                                System.out.println("//�õ��������Ŀͻ������ӣ����Խ��пͻ����첽����");

                                synchronized (TCPServer.this) {
                                    clientHandles.add(clientHandler);
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
            } while (!done);
            System.out.println("�������ѹرա�");
        }

        void exit() {
            done = true;
            //���ѵ�ǰ������
            selector.wakeup();
        }
    }
}
