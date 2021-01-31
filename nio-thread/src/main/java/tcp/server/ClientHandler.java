package tcp.server;

import tcp.utils.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class ClientHandler {
    private SocketChannel socketChannel;
    private ReadHandler readHandler;
    private WriteHandler writeHandler;
    private ClientHandlerCallback clientHandlerCallback;

    private Selector readSelector;
    private Selector writeSelector;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);

        readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        this.readHandler = new ReadHandler(readSelector);

        writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new WriteHandler(writeSelector);
        this.clientHandlerCallback = clientHandlerCallback;
    }

    public void readToPrint() {
        readHandler.start();
    }

    public void send(String message) {
        writeHandler.send(message);
    }

    public void exitByItself() {
        exit();
        clientHandlerCallback.onSelfClosed(ClientHandler.this);
    }

    public void exit() {
        readHandler.close();
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已经退出");
    }

    class WriteHandler {
        ExecutorService executorService;
        private boolean done;
        private ByteBuffer byteBuffer;

        private Selector selector;

        public WriteHandler(Selector selector) {
            this.selector = selector;
            this.executorService = Executors.newSingleThreadExecutor();
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        private void send(String message) {
            executorService.execute(new WriteRunnable(message));
        }

        private void exit() {
            done = true;
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private String message;

            public WriteRunnable(String message) {
                this.message = message + '\n';
            }

            @Override
            public void run() {
                if (WriteHandler.this.done) {
                    return;
                }
                byteBuffer.clear();
                byteBuffer.put(message.getBytes());
                byteBuffer.flip();
                while (!done && byteBuffer.hasRemaining()) {
                    try {
                        int len = socketChannel.write(byteBuffer);
                        if (len < 0) {
                            System.out.println("客户端已无法发送数据");
                            exitByItself();
                        }
                    } catch (IOException e) {
                        System.out.println("客户端连接异常: " + e.getMessage());
                        exitByItself();
                    }
                }
            }
        }

    }

    public interface ClientHandlerCallback {
        // 自身关闭通知
        void onSelfClosed(ClientHandler handler);

        // 收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);
    }


    class ReadHandler extends Thread {
        private volatile boolean done;
        private Selector selector;
        private ByteBuffer byteBuffer;

        public ReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run() {
            try {

                while (!done) {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        byteBuffer.clear();
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isReadable()) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            int read = channel.read(byteBuffer);
                            if (read > 0) {
                                //丢弃最后一个换行符
                                String str = new String(byteBuffer.array(), 0, byteBuffer.position() - 1);
                                System.out.println(str);
                                clientHandlerCallback.onNewMessageArrived(ClientHandler.this, str);
                            } else {
                                System.out.println("客户端已无法读取数据");
                                ClientHandler.this.exitByItself();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                close();
            }

        }

        private void close() {
            done = true;
            selector.wakeup();
            CloseUtils.close(selector);
        }
    }
}
