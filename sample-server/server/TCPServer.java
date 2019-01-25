package server;

import server.handle.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author:zhumeng
 * @desc:
 **/
public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener mListener;

    //�̰߳�ȫ����֤ɾ��ʱ����ӣ�������ȫ
    private List<ClientHandler> clientHandles = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService forwardingThreadPollExecutor;

    public TCPServer(int port) {

        this.port = port;
        this.forwardingThreadPollExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            ClientListener listener = new ClientListener(port);
            mListener = listener;
            listener.start();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    public void stop() {
        if (mListener != null) {
            mListener.exit();
        }
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
    public void onNewMessageArrive(final ClientHandler handler, final String msg) {
        //��ӡ����Ļ��
        System.out.println("Received-" + handler.getClientInfo() + ":" + msg);
        //�첽�ύת������
        forwardingThreadPollExecutor.execute(() -> {
            for (ClientHandler clientHandler : clientHandles) {
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
        private ServerSocket server;
        private boolean done = false;

        public ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.println("��������Ϣ��" + server.getInetAddress() + "\tP" + server.getLocalPort());

        }

        @Override
        public void run() {
            super.run();
            System.out.println("������׼��������");
            //�ȴ��ͻ�������
            do {
                //�õ��ͻ���
                Socket client = null;
                try {
                    client = server.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                }
                //�ͻ��˹����첽�߳�
                ClientHandler clientHandle = null;
                try {
                    clientHandle = new ClientHandler(client, TCPServer.this);
                    //��ȡ���ݲ���ӡ
                    clientHandle.readToPrint();
                    synchronized (TCPServer.this) {
                        clientHandles.add(clientHandle);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("�ͻ��������쳣" + e.getMessage());
                }

            } while (!done);
            System.out.println("�������ѹرա�");
        }

        void exit() {
            done = true;
            try {
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
