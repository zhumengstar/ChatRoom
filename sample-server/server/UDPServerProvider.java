package server;

import com.clink.utils.ByteUtils;
import constants.UDPConstants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author:zhumeng
 * @desc:
 **/
public class UDPServerProvider {

    private static Provider PROVIDER_INSTANCE;

    public UDPServerProvider() {
    }

    public static void start(int portServer) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, portServer);


        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    public static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class Provider extends Thread {

        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;
        //存储消息的buffer
        final byte[] buffer = new byte[128];

        public Provider(String sn, int port) {
            super();
            this.sn = sn.getBytes();
            this.port = port;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("UDPProvider Started.");

            try {
                //监听20000端口
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);

                //接收消息的Packet
                DatagramPacket receiver = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    //接收消息
                    ds.receive(receiver);

                    //打印接收者消息和发送者消息
                    //发送者的ip地址
                    String clientIp = receiver.getAddress().getHostAddress();
                    int clientPort = receiver.getPort();
                    int clientDataLen = receiver.getLength();
                    byte[] clientData = receiver.getData();
                    boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4)
                            && ByteUtils.startsWith(clientData, UDPConstants.HEADER);
                    System.out.println("ServerProvider receiver from ip:" + clientIp + "\tport:" + clientPort + "\tdataValid:" + isValid);

                    if (!isValid) {
                        continue;
                    }
                    //解析命令与回送端口
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));

                    int responsePort = (((clientData[index++]) << 24) |
                            ((clientData[index++] & 0xff) << 16) |
                            ((clientData[index++] & 0xff) << 8) |
                            ((clientData[index++] & 0xff)));

                    //判断合法性
                    if (cmd == 1 && responsePort > 0) {
                        //构建一份回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);

                        int len = byteBuffer.position();

                        //直接根据发送者构建一份回送消息
                        DatagramPacket responsePack = new DatagramPacket(buffer, len, receiver.getAddress(), responsePort);

                        ds.send(responsePack);

                        System.out.println("ServerProvider response to:" + clientIp + "\tport:" + responsePort + "\tdataLen:" + len);

                    } else {
                        System.out.println("ServerProvider receive cmd nonsupport;cmd::" + cmd + "\tport:" + port);

                    }
                }


            } catch (Exception ignore) {
            } finally {
                close();
            }
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        //结束函数
        private void exit() {
            done = true;
            close();
        }
    }
}
