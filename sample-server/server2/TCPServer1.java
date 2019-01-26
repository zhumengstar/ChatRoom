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
    //nio监听
    private ClientListener listener;

    //线程安全，保证删除时，添加，遍历安全
    private List<ClientHandler1> clientHandles = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService forwardingThreadPollExecutor;

    //选择器
    private Selector selector;
    //
    private ServerSocketChannel server;

    public TCPServer1(int port) {

        this.port = port;
        //转发线程池
        this.forwardingThreadPollExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {


            selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            //设置为非阻塞
            server.configureBlocking(false);
            //绑定本地端口
            server.socket().bind(new InetSocketAddress(port));
            this.server = server;
            //注册客户端连接到达监听
            server.register(selector, SelectionKey.OP_ACCEPT);


            System.out.println("服务器信息：" + server.getLocalAddress().toString());

            //启动客户端监听
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

        //同步处理线程安全
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
        //打印到屏幕上
        System.out.println("Received-" + handler.getClientInfo() + ":" + msg);
        //异步提交转发任务
        forwardingThreadPollExecutor.execute(() -> {
            for (ClientHandler1 clientHandler : clientHandles) {
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
        private boolean done = false;


        @Override
        public void run() {
            super.run();


            Selector selector = TCPServer1.this.selector;


            System.out.println("服务器准备就绪～");
            //等待客户端连接
            do {

                try {
                    //永远阻塞select
                    //select表示当前有没有事件就绪，数量有多少
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

                        //检查当前key的状态是否时我们关注的客户端到达状态
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            //拿到非阻塞的客户端连接，可以进行客户端异步操作
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            try {
                                //客户端构建异步线程
                                ClientHandler1 clientHandle = new ClientHandler1(socketChannel, TCPServer1.this);
                                //读取数据并打印
                                clientHandle.readToPrint();
                                synchronized (TCPServer1.this) {
                                    clientHandles.add(clientHandle);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常" + e.getMessage());
                            }
                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            while (!done);
            System.out.println("服务器已关闭～");
        }

        void exit() {
            done = true;
            //唤醒当前的阻塞
            selector.wakeup();
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
