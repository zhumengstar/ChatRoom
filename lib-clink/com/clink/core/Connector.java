package com.clink.core;

import javax.sound.midi.Receiver;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @author:zhumeng
 * @desc:
 **/
public class Connector {

    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;
    }
}
