package com.clink.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author:zhumeng
 * @desc:
 **/
public class IoArgs {

    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        //����buffer
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException {
        //д��buffer
        return channel.write(buffer);
    }

    //�������з�
    public String bufferString() {
        return new String(byteBuffer, 0, buffer.position() - 1);
    }

    // ����IoArs״̬
    public interface IoArgsEventListener {
        void onStarted(IoArgs args);

        void onCompleted(IoArgs args);
    }
}
