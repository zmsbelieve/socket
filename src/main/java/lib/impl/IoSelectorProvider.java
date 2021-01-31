package lib.impl;

import lib.core.IoProvider;
import tcp.utils.CloseUtils;

import java.io.IOException;
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

public class IoSelectorProvider implements IoProvider {

    private AtomicBoolean isClosed = new AtomicBoolean(false);

    private Selector readSelector;
    private Selector writeSelector;
    private ExecutorService inputHandlePool;
    private ExecutorService outputHandlePool;

    private AtomicBoolean inRegInput = new AtomicBoolean(false);
    private AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private Map<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private Map<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();

        inputHandlePool = Executors.newFixedThreadPool(4, new SelectorThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4, new SelectorThreadFactory("IoProvider-Output-Thread-"));

        startRead();
        startWrite();
    }

    private void startRead() {
        Thread thread = new Thread("startRead start") {
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
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        //最优先执行的线程
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void handleSelection(SelectionKey key, int ops, Map<SelectionKey, Runnable> callbackMap, ExecutorService pool) {
        //取消监听
        key.interestOps(key.interestOps() & ~ops);
        Runnable runnable = callbackMap.get(key);
        if (runnable != null && !pool.isShutdown()) {
            pool.execute(runnable);
        }
    }

    private void startWrite() {
        Thread thread = new Thread("startWrite start") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(inRegOutput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
                            }
                        }
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
    public boolean registerInput(SocketChannel channel, HandleInputCallback handleInputCallback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, handleInputCallback) != null;
    }

    private void waitSelection(AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {//这块感觉没必要设置locker的状态?
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private SelectionKey registerSelection(SocketChannel channel, Selector selector, int ops, AtomicBoolean locker, Map<SelectionKey, Runnable> inputCallbackMap, Runnable runnable) {
        synchronized (locker) {
            locker.set(true);//这块感觉没必要设置locker的状态?
            try {
                selector.wakeup();//这边唤醒是为了刷新selector
                SelectionKey key = null;
                try {
                    if (channel.isRegistered()) {
                        key = channel.keyFor(selector);
                        if (key != null) {
                            key.interestOps(key.interestOps() | ops);
                        }
                    }
                    if (key == null) {
                        key = channel.register(selector, ops);
                        inputCallbackMap.put(key, runnable);
                    }
                    return key;
                } catch (Exception e) {
                    return null;
                }
            } finally {
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception ignore) {

                }
            }
        }
    }

    private void unRegisterSelection(SocketChannel channel, Selector selector, Map<SelectionKey, Runnable> callbackMap) {
        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                key.cancel();
                callbackMap.remove(key);
                //可以理解为刷新selector的select()操作，让其获取新的selectionKey进行select
                selector.wakeup();
            }
        }
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback handleOutputCallbak) {
        return registerSelection(channel, readSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, handleOutputCallbak) != null;
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

            CloseUtils.close(readSelector, writeSelector);
        }
    }

    static class SelectorThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        public String namePrefix;

        SelectorThreadFactory(String namePrefix) {
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
