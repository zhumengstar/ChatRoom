package com.clinke.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

/**
 * @author:zhumeng
 * @desc:
 **/
public interface IoProvider extends Closeable {
    //观察者模式
    boolean registerInput(SocketChannel channel, HandlerInputCallBack callBack);//回调

    boolean registerOutput(SocketChannel channel, HandlerOutputCallBack callBack);

    //解绑
    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);


    abstract class HandlerInputCallBack implements Runnable {
        @Override
        public void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }

    abstract class HandlerOutputCallBack implements Runnable {
        //副本
        private Object attach;

        @Override
        public void run() {
            canProviderOutput(attach);
        }

        //
        public final void setAttach(Object attach) {
            this.attach = attach;
        }

        protected abstract void canProviderOutput(Object attach);

    }
}
