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
        //¶Á½øbuffer
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException {
        //Ð´½øbuffer
        return channel.write(buffer);
    }

    //¶ªÆú»»ÐÐ·û
    public String bufferString() {
        return new String(byteBuffer, 0, buffer.position() - 1);
    }

    // ¼àÌýIoArs×´Ì¬
    public interface IoArgsEventListener {
        void onStarted(IoArgs args);

        void onCompleted(IoArgs args);
    }
}
