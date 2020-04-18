package Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server implements Server_I{
    int MAX_CONNECTIONS = 1;
    String HOST = "localhost";
    int PORT = 20;
    ThreadPoolExecutor threadPoolExecutor;
    ThreadFactory socketFactory;
    ServerSocketChannel serverSocket;
    boolean isOn = true;
    ThreadPool pool;

    private Server(){
        setThreadPoolExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_CONNECTIONS));
    }

    static Server instance = new Server(20, 1);

    public static Server getInstance(){
        return instance;
    }

    private Server(int port, int max_connections){
        PORT = port;
        MAX_CONNECTIONS = max_connections;
        setThreadPoolExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_CONNECTIONS));
        pool = new ThreadPool();
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
        int i = 0;
        //threadPoolExecutor.setKeepAliveTime(2000, TimeUnit.MILLISECONDS);
        while(isOn){
            if(threadPoolExecutor.getQueue().size()>10){
                threadPoolExecutor.getQueue().forEach(k->threadPoolExecutor.remove(k));
            }
            SocketChannel socket = serverSocket.accept();
            socket.socket().setKeepAlive(true);
            threadPoolExecutor.execute(new Task(socket,++i));
            System.out.println("PoolSize: " + threadPoolExecutor.getPoolSize());
            System.out.println("Active: " + threadPoolExecutor.getActiveCount());
            System.out.println("Queue: " + threadPoolExecutor.getQueue().size());

        }
        //serverSocket.configureBlocking(false);
        //pool.register(serverSocket, SelectionKey.OP_ACCEPT, threadPoolExecutor);
    }

    public synchronized ThreadPool getSelectorsPool(){
        return pool;
    }

    @Override
    public void shutdown() {
        isOn = false;
        threadPoolExecutor.shutdownNow();
    }

}
