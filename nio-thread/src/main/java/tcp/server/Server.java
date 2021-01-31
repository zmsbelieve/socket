package tcp.server;

import lib.core.IoContext;
import lib.impl.IoSelectorProvider;
import tcp.constants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class Server {
    public static void main(String[] args) throws IOException {
        //new IoSelectorProvider的时候就开始进行 read 和 write的监听了
        IoContext.setup().ioProvider(new IoSelectorProvider())
                .start();
        TcpServer tcpServer = new TcpServer(TCPConstants.PORT_SERVER);
        boolean isSuccess = tcpServer.start();
        if (!isSuccess) {
            System.out.println("tcpServer start  fail!");
            return;
        }
        System.out.println("tcpServer start success!");
        UDPProvider udpProvider = new UDPProvider();
        udpProvider.start(TCPConstants.PORT_SERVER);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String str = bufferedReader.readLine();
                if ("bye".equals(str)) {
                    break;
                }
                tcpServer.sendBroadcast(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        udpProvider.stop();
        tcpServer.stop();
        IoContext.close();
    }
}
