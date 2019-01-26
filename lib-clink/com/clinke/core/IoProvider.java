package com.clinke.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

/**
 * @author:zhumeng
 * @desc:
 **/
public interface IoProvider extends Closeable {
    //�۲���ģʽ
    boolean registerInput(SocketChannel channel, HandlerInputCallBack callBack);//�ص�

    boolean registerOutput(SocketChannel channel, HandlerOutputCallBack callBack);

    //���
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
        //����
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
