package com.clinke.core;

import java.io.IOException;

/**
 * @author:zhumeng
 * @desc:
 **/
public class IoContext {
    //������̬
    private static IoContext INSTANCE;

    //���ȫ������
    private final IoProvider ioProvider;

    public IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public static IoContext get() {
        return INSTANCE;
    }

    public static StartedBoot setup() {
        return new StartedBoot();
    }

    public static void close() throws IOException {
        if (INSTANCE != null) {
            INSTANCE.callClose();
        }
    }


    private void callClose() throws IOException {
        ioProvider.close();
    }

    public static class StartedBoot {
        private IoProvider ioProvider;

        public StartedBoot() {
        }

        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IoContext start() {
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }
    }

}
