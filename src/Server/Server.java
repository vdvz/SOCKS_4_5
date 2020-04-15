package Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public class Server implements Server_I{
    int MAX_CONNECTIONS = 10;
    String HOST = "localhost";
    int PORT = 20;
    ThreadPoolExecutor threadPoolExecutor;
    ThreadFactory socketFactory;
    ServerSocketChannel serverSocket;
    boolean isOn = true;


    Server(){
        setThreadPoolExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_CONNECTIONS));
    }

    public Server(int port, int max_connections){
        PORT = port;
        MAX_CONNECTIONS = max_connections;
        setThreadPoolExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_CONNECTIONS));
    }

    @Override
    public void setPort(int port){
        PORT = port;
    }

    @Override
    public int getPort(){
        return PORT;
    }

    @Override
    public int getMaximumConnections(){
        return MAX_CONNECTIONS;
    }

    @Override
    public void setMaximumConnections(int count_connections){
        MAX_CONNECTIONS = count_connections;
    }

    @Override
    public void setThreadPoolExecutor(ThreadPoolExecutor executor){
        threadPoolExecutor = executor;
    }

    @Override
    public ThreadPoolExecutor getThreadPoolExecutor(){
        return threadPoolExecutor;
    }

    @Override
    public void setSocketFactory(ThreadFactory factory){
        socketFactory = factory;
        threadPoolExecutor.setThreadFactory(socketFactory);
    };

    @Override
    public ThreadFactory getSocketFactory(){
        return socketFactory;
    }

    @Override
    public void configurate() throws IOException {
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(HOST, PORT));
    }

    @Override
    public void turn_off_server(){
        isOn = false;
    }

    @Override
    public void start() throws IOException {
        while(isOn) {
            SocketChannel client = serverSocket.accept();
            threadPoolExecutor.execute(new Connection(client));
        }
        shutdown();
    }

    @Override
    public void shutdown() {
        isOn = false;
        threadPoolExecutor.shutdownNow();
        BD.getInstance().clear();
    }

}
