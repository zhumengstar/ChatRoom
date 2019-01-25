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

    //线程安全，保证删除时，添加，遍历安全
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
        //同步处理线程安全
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
        //打印到屏幕上
        System.out.println("Received-" + handler.getClientInfo() + ":" + msg);
        //异步提交转发任务
        forwardingThreadPollExecutor.execute(() -> {
            for (ClientHandler clientHandler : clientHandles) {
                if (clientHandler.equals(handler)) {
                    //跳过自己
                    continue;
                }
                //发给其他
                clientHandler.send(msg);
            }
        });


    }

    private class ClientListener extends Thread {
        private ServerSocket server;
        private boolean done = false;

        public ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.println("服务器信息：" + server.getInetAddress() + "\tP" + server.getLocalPort());

        }

        @Override
        public void run() {
            super.run();
            System.out.println("服务器准备就绪～");
            //等待客户端连接
            do {
                //得到客户端
                Socket client = null;
                try {
                    client = server.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                }
                //客户端构建异步线程
                ClientHandler clientHandle = null;
                try {
                    clientHandle = new ClientHandler(client, TCPServer.this);
                    //读取数据并打印
                    clientHandle.readToPrint();
                    synchronized (TCPServer.this) {
                        clientHandles.add(clientHandle);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("客户端连接异常" + e.getMessage());
                }

            } while (!done);
            System.out.println("服务器已关闭～");
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

    //客户端消息处理
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
//            System.out.println("新客户端连接：" + socket.getInetAddress() + "P:" + socket.getLocalPort());
//
//            try {
//                //得到打印流，用于数据输出，服务器回送数据使用
//                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
//                //得到输入流，用于接收数据
//                BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//                do {
//                    //客户端拿到数据
//                    String str = socketInput.readLine();
//                    if ("bye".equalsIgnoreCase(str)) {
//                        flag = false;
//                        //回送
//                        System.out.println("bye");
//                        socketOutput.println("bye");
//
//                    } else {
//                        //打印到屏幕，并回送数据长度
//                        if (str != null) {
//                            System.out.println(str);
//                        }
//
//                        //发回客户端
//                        socketOutput.println("回送长度:" + str.length());
//                    }
//                } while (flag);
//
//                socketInput.close();
//                socketOutput.close();
//
//            } catch (Exception e) {
//                System.out.println("连接异常断开～");
//            } finally {
//                //连接关闭
//                try {
//                    socket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("客户端已退出" + socket.getInetAddress() + "P:" + socket.getPort());
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
