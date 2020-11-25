package tcp.server;

import tcp.utils.CloseUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class TcpServer {
    private int port;
    private ServerListener serverListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();

    public TcpServer(int port) {
        this.port = port;

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
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.exit();
        }
        clientHandlerList.clear();
    }

    public void sendBroadcast(String message) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(message);
        }
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
                    ClientHandler clientHandler = new ClientHandler(socket);
                    clientHandler.readToPrint();
                    clientHandlerList.add(clientHandler);
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
