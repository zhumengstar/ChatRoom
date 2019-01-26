package server.handle;

import com.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author:zhumeng
 * @desc:
 **/
public class ClientHandler1 {
    private SocketChannel socketChannel;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    public ClientHandler1(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socketChannel = socketChannel;
        //设置非阻塞模式
        socketChannel.configureBlocking(false);

        Selector readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        this.readHandler = new ClientReadHandler(readSelector);

        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);


        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();

        System.out.println("新客户端连接：" + clientInfo);

    }


    public String getClientInfo() {
        return clientInfo;
    }


    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出" + clientInfo);
    }

    public void send(String str) {
        writeHandler.send(str);
    }

    public void readToPrint() {
        readHandler.start();
    }

    private void exitBySelf() {
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        //自身关闭通知
        void onSelfClosed(ClientHandler1 handler);

        //收到消息时侯通知
        void onNewMessageArrive(ClientHandler1 handler, String msg);

    }

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final Selector selector;

        private final ByteBuffer byteBuffer;

        ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }


        @Override
        public void run() {
            super.run();

            try {
                //得到打印流，用于数据输出，服务器回送数据使用
//                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                //得到输入流，用于接收数据
//                BufferedReader socketInput = new BufferedReader(new InputStreamReader());

                do {
                    //客户端拿到数据
                    //String str = socketInput.readLine();
                    System.out.println("================");
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

                        if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            //清空
                            byteBuffer.clear();

                            int read = client.read(byteBuffer);
                            if (read > 0) {
                                //丢弃换行符
                                String str = new String(byteBuffer.array(), 0, read - 1);
                                //通知到TCPServer
                                clientHandlerCallback.onNewMessageArrive(ClientHandler1.this, str);
                            } else {
                                System.out.println("客户端已无法读取数据");
                                //退出客户端
                                ClientHandler1.this.exitBySelf();
                                break;
                            }
                        }
                    }
                } while (!done);

            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开～");
                    ClientHandler1.this.exitBySelf();
                }
            } finally {
                CloseUtils.close(selector);
            }
        }

        void exit() {
            done = true;
            //若为阻塞状态需要唤醒
            selector.wakeup();
            CloseUtils.close(selector);
        }
    }

    class ClientWriteHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        private final ExecutorService executorService;


        ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void exit() {
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        void send(String str) {

            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        class WriteRunnable implements Runnable {
            private final String msg;

            WriteRunnable(String msg) {
                this.msg = msg + '\n';
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                byteBuffer.clear();

                byteBuffer.put(msg.getBytes());
                //反转操作，重点
                byteBuffer.flip();

                while (!done && byteBuffer.hasRemaining()) {


                    try {

                        int write = socketChannel.write(byteBuffer);
//                        write=0是合法的
                        if (write < 0) {
                            System.out.println("客户端已无法发送数据");
                            ClientHandler1.this.exitBySelf();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
