package tcp.server;

import tcp.utils.CloseUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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

    public TcpServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            serverListener = new ServerListener(port);
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
        private ServerSocket serverSocket;

        public ServerListener(int port) throws IOException {
            serverSocket = new ServerSocket(port);
        }

        @Override
        public void run() {
            while (!done) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket, TcpServer.this);
                    clientHandler.readToPrint();
                    synchronized (TcpServer.this) {
                        clientHandlerList.add(clientHandler);
                    }
                } catch (IOException e) {


                }
            }
        }

        public void exit() {
            done = true;
            CloseUtils.close(serverSocket);
        }
    }
}
