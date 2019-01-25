package server.handle;

import com.clink.utils.CloseUtils;
import server.TCPServer;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author:zhumeng
 * @desc:
 **/
public class ClientHandler {
    private Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    public ClientHandler(Socket socket, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socket = socket;
        this.readHandler = new ClientReadHandler(socket.getInputStream());
        this.writeHandler = new ClientWriteHandler(socket.getOutputStream());
        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = "A[" + socket.getInetAddress().getHostAddress() + "] P[" + socket.getPort() + "]";

        System.out.println("�¿ͻ������ӣ�" + clientInfo);

    }


    public String getClientInfo() {
        return clientInfo;
    }


    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socket);
        System.out.println("�ͻ������˳�" + socket.getInetAddress() + "P:" + socket.getPort());
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
        void onSelfClosed(ClientHandler handler);

        //�յ���Ϣʱ��֪ͨ
        void onNewMessageArrive(ClientHandler handler, String msg);

    }

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ClientReadHandler(InputStream inputStream) {
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
                    //�ͻ����õ�����
                    String str = socketInput.readLine();

                    if (str == null) {
                        System.out.println("�ͻ������޷���ȡ����");
                        //�˳��ͻ���
                        ClientHandler.this.exitBySelf();
                        break;
                    }


                    //��ӡ����Ļ
                    System.out.println(str);
                    clientHandlerCallback.onNewMessageArrive(ClientHandler.this, str);


                } while (!done);


            } catch (Exception e) {
                if (!done) {
                    System.out.println("�����쳣�Ͽ���");
                    ClientHandler.this.exitBySelf();
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

    class ClientWriteHandler {
        private boolean done = false;
        private final PrintStream printStream;
        private final ExecutorService executorService;

        ClientWriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);

            this.executorService = Executors.newSingleThreadExecutor();
        }

        void exit() {
            done = true;
            CloseUtils.close(printStream);
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
                this.msg = msg;
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }
                try {
                    ClientWriteHandler.this.printStream.println(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
