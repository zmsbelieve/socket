package tcp.client;

import tcp.client.bean.ServerInfo;

import java.io.IOException;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class Client {

    public static void main(String[] args) throws IOException, InterruptedException {
        //UDP搜索tcp连接信息
        ServerInfo serverInfo = UDPSearch.search(5);
        //tcp连接
        if (serverInfo != null) {
            TCPClient tcpClient = new TCPClient();
            tcpClient.linkWith(serverInfo);
        }
    }
}
