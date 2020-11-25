package tcp.client;

import tcp.client.bean.ServerInfo;
import tcp.utils.CloseUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class TCPClient {

    public void linkWith(ServerInfo serverInfo) throws IOException {
        System.out.println("开始进行tcp客户端的连接...");
        Socket socket = new Socket();
        socket.setSoTimeout(3000);
        socket.connect(new InetSocketAddress(serverInfo.getAddress(), serverInfo.getPort()), 3000);
        ReadHandler readHandler = new ReadHandler(socket.getInputStream());
        readHandler.start();
        write(socket.getOutputStream());
        readHandler.close();
    }

    private void write(OutputStream outputStream) {
        InputStream in = System.in;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        PrintStream printStream = new PrintStream(outputStream);
        while (true) {
            try {
                String str = bufferedReader.readLine();
                if ("bye".equals(str)) {
                    break;
                }
                printStream.println(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        printStream.close();
    }

    class ReadHandler extends Thread {
        private InputStream inputStream;
        private volatile boolean done;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (!done) {
                try {
                    String str = reader.readLine();
                    System.out.println(str);
                } catch (IOException e) {
                    System.out.println("读取失败........");
                    close();
                }
            }
        }

        private void close() {
            done = false;
            CloseUtils.close(inputStream);
        }
    }
}
