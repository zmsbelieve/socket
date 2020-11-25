package tcp.server;

import tcp.constants.TCPConstants;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class Server {
    public static void main(String[] args) {
        TcpServer tcpServer = new TcpServer(TCPConstants.PORT_SERVER);
        UDPProvider udpProvider = new UDPProvider();

        udpProvider.start(TCPConstants.PORT_SERVER);
    }
}
