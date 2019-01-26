package com.clink.Impl;

import com.clink.core.IoArgs;
import com.clink.core.IoProvider;
import com.clink.core.Receiver;
import com.clink.core.Sender;
import com.clink.utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author:zhumeng
 * @desc:
 **/
public class SocketChannelAdapter implements Sender, Receiver, Closeable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final SocketChannel channel;

    private final IoProvider ioProvider;

    private final OnChannelStatusChangeListener listener;

    private IoArgs.IoArgsEventListener receiveIoEventListener;
    private IoArgs.IoArgsEventListener sendIoEventListener;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangeListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }


    @Override
    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {

        if (!isClosed.get()) {
            throw new IOException("Current channel is closed.");
        }
        receiveIoEventListener = listener;


        return ioProvider.registerInput(channel, inputCallBack);
    }

    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {
        if (!isClosed.get()) {
            throw new IOException("Current channel is closed.");
        }
        sendIoEventListener = listener;

        //当前发送的数据附加到回调中
        outputCallBack.setAttach(args);


        return ioProvider.registerOutput(channel, outputCallBack);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            //关闭
            CloseUtils.close(channel);
            listener.OnChannelClosed(channel);
        }
    }

    private final IoProvider.HandlerInputCallBack inputCallBack = new IoProvider.HandlerInputCallBack() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }
            IoArgs args = new IoArgs();
            IoArgs.IoArgsEventListener listener = SocketChannelAdapter.this.receiveIoEventListener;

            if (listener != null) {
                listener.onStarted(args);
            }
            try {
                //具体的读取操作
                if (args.read(channel) > 0 && listener != null) {
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot read any data!");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandlerOutputCallBack outputCallBack = new IoProvider.HandlerOutputCallBack() {
        @Override
        protected void canProviderOutput(Object attach) {
            if (isClosed.get()) {
                return;
            }
            //TODO
            sendIoEventListener.onCompleted(null);
        }
    };

    public interface OnChannelStatusChangeListener {
        void OnChannelClosed(SocketChannel channel);
    }
}
