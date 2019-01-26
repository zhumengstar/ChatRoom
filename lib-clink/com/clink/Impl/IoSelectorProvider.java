package com.clink.Impl;

import com.clink.core.IoProvider;
import com.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author:zhumeng
 * @desc:
 **/
public class IoSelectorProvider implements IoProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    //�Ƿ���ĳ������
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandlePool;
    private final ExecutorService outputHandlePool;


    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();
        inputHandlePool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Output-Thread-"));
        //��ʼ�������ļ���
        startRead();
        startWrite();
    }


    private void startRead() {
        Thread thread = new Thread("Clink IoSelectorProvider ReadSelector") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(inRegInput);

                            continue;
                        }
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                //�ص�
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }


    private void startWrite() {
        Thread thread = new Thread("Clink IoSelectorProvider Write Selector") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        //�Ƿ�����
                        if (writeSelector.select() == 0) {
                            waitSelection(inRegOutput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                //�ص�
                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }


    @Override
    public boolean registerInput(SocketChannel channel, HandlerInputCallBack callBack) {

        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callBack) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandlerOutputCallBack callBack) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callBack) != null;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap);
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {

            inputHandlePool.shutdown();
            outputHandlePool.shutdown();
            inputCallbackMap.clear();
            outputCallbackMap.clear();


            readSelector.wakeup();
            writeSelector.wakeup();
            CloseUtils.close(readSelector, writeSelector);
        }
    }

    private static void waitSelection(final AtomicBoolean locker){
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker){
            if(!locker.get()){
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static SelectionKey registerSelection(SocketChannel channel,
                                                  Selector selector,
                                                  int registerOps,
                                                  AtomicBoolean locker,
                                                  HashMap<SelectionKey, Runnable> map,
                                                  Runnable runnable) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker) {
            //����״̬
            locker.set(true);
            try {
                //���� ���ѵ�ǰ��selector����selector������select()״̬
                selector.wakeup();
                SelectionKey key = null;
                if (channel.isRegistered()) {
                    //��ѯ�Ƿ�ע���
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);

                    }

                }
                if (key == null) {
                    //ע��Selector�õ�key
                    key = channel.register(selector, registerOps);
                    //ע��ص�
                    map.put(key, runnable);
                }

                //���ص�ǰ��key
                return key;
            } catch (ClosedChannelException e) {
                return null;
            } finally {
                //�������״̬
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception ignored) {

                }
            }
        }

    }

    private static void unRegisterSelection(SocketChannel channel,
                                            Selector selector,
                                            Map<SelectionKey, Runnable> map) {

        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                //ȡ�������ķ�����ȡ�������¼�
                key.cancel();
                map.remove(key);
                selector.wakeup();
            }
        }

    }

    private static void handleSelection(SelectionKey key, int keyOps,
                                        HashMap<SelectionKey, Runnable> map,
                                        ExecutorService pool) {
        //�ص�
        //ȡ��������keyOps�ļ�����������һ��
        key.interestOps(key.readyOps() & ~keyOps);

        Runnable runnable = null;
        try {
            runnable = map.get(key);


        } catch (Exception e) {

        }

        if (runnable != null && !pool.isShutdown()) {
            //runnable �첽����
            pool.execute(runnable);
        }


    }


    static class IoProviderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
