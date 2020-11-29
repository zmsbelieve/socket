package tcp.client;

import tcp.client.bean.ServerInfo;
import tcp.utils.CloseUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class TCPClient {
    private Socket socket;
    private ReadHandler readHandler;
    private PrintStream printStream;

    public TCPClient(Socket socket, ReadHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public static TCPClient startWith(ServerInfo serverInfo) throws IOException {
        System.out.println("开始进行tcp客户端的连接...");
        Socket socket = new Socket();
        socket.setSoTimeout(3000);
        socket.connect(new InetSocketAddress(serverInfo.getAddress(), serverInfo.getPort()), 3000);
        ReadHandler readHandler = new ReadHandler(socket.getInputStream());
        readHandler.start();
        return new TCPClient(socket, readHandler);
    }

    public void send(String msg) {
        this.printStream.println(msg);
    }

    public void stop() {
        readHandler.close();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }

    static class ReadHandler extends Thread {
        private InputStream inputStream;
        private volatile boolean done;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                while (!done) {
                    String str;
                    try {
                        str = reader.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    if (str == null) {
                        System.out.println("连接已关闭，无法读取数据！");
                        break;
                    }
                    System.out.println(str);
                }
            } catch (Exception e) {
                if (!done) {
                    System.out.println("非正常断开：" + e.getMessage());
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }

        private void close() {
            done = false;
            CloseUtils.close(inputStream);
        }
    }
}
