package com.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author:zhumeng
 * @desc:
 **/
public class IoContext implements Closeable {
    //单例静态
    private static IoContext INSTANCE;

    //针对全局连接
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

    @Override
    public void close() throws IOException {
        ioProvider.close();
    }

    private static class StartedBoot {
        private IoProvider ioProvider;

        public StartedBoot() {
        }

        public StartedBoot ioPrivoder(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IoContext start() {
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }
    }

}
