package tcp.server;

import tcp.utils.CloseUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class TcpServer implements ClientHandler.ClientHandlerCallback {
    private int port;
    private ServerListener serverListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;

    private Selector selector;
    private ServerSocketChannel server;

    public TcpServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT);

            serverListener = new ServerListener();
            serverListener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (serverListener != null) {
            serverListener.exit();
        }
        CloseUtils.close(server);
        CloseUtils.close(selector);
        synchronized (TcpServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }
        forwardingThreadPoolExecutor.shutdownNow();
    }

    public void sendBroadcast(String message) {
        synchronized (TcpServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.send(message);
            }
        }
    }

    @Override
    public void onSelfClosed(ClientHandler handler) {
        synchronized (TcpServer.this) {
            clientHandlerList.remove(handler);
        }
    }

    @Override
    public void onNewMessageArrived(ClientHandler handler, String msg) {
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TcpServer.this) {
                for (ClientHandler clientHandler : clientHandlerList) {
                    if (clientHandler == handler) {
                        continue;
                    }
                    clientHandler.send(msg);
                }
            }
        });
    }

    class ServerListener extends Thread {
        private volatile boolean done;

        @Override
        public void run() {
            while (!done) {
                try {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()) {
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel channel = server.accept();
                            try {
                                ClientHandler clientHandler = new ClientHandler(channel, TcpServer.this);
                                synchronized (TcpServer.this) {
                                    clientHandlerList.add(clientHandler);
                                }
                            } catch (IOException e) {
                                System.out.println("客户端连接异常: " + e.getMessage());
                            }
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void exit() {
            done = true;
            //唤醒当前的阻塞
            selector.wakeup();
        }
    }
}
