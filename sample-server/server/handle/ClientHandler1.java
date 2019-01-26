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
        //���÷�����ģʽ
        socketChannel.configureBlocking(false);

        Selector readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        this.readHandler = new ClientReadHandler(readSelector);

        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);


        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();

        System.out.println("�¿ͻ������ӣ�" + clientInfo);

    }


    public String getClientInfo() {
        return clientInfo;
    }


    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("�ͻ������˳�" + clientInfo);
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
        //����ر�֪ͨ
        void onSelfClosed(ClientHandler1 handler);

        //�յ���Ϣʱ��֪ͨ
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
                //�õ���ӡ�������������������������������ʹ��
//                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                //�õ������������ڽ�������
//                BufferedReader socketInput = new BufferedReader(new InputStreamReader());

                do {
                    //�ͻ����õ�����
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
                            //���
                            byteBuffer.clear();

                            int read = client.read(byteBuffer);
                            if (read > 0) {
                                //�������з�
                                String str = new String(byteBuffer.array(), 0, read - 1);
                                //֪ͨ��TCPServer
                                clientHandlerCallback.onNewMessageArrive(ClientHandler1.this, str);
                            } else {
                                System.out.println("�ͻ������޷���ȡ����");
                                //�˳��ͻ���
                                ClientHandler1.this.exitBySelf();
                                break;
                            }
                        }
                    }
                } while (!done);

            } catch (Exception e) {
                if (!done) {
                    System.out.println("�����쳣�Ͽ���");
                    ClientHandler1.this.exitBySelf();
                }
            } finally {
                CloseUtils.close(selector);
            }
        }

        void exit() {
            done = true;
            //��Ϊ����״̬��Ҫ����
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
                //��ת�������ص�
                byteBuffer.flip();

                while (!done && byteBuffer.hasRemaining()) {


                    try {

                        int write = socketChannel.write(byteBuffer);
//                        write=0�ǺϷ���
                        if (write < 0) {
                            System.out.println("�ͻ������޷���������");
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
