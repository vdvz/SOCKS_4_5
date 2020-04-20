package Server;

import Server.SelectorPool.ThreadPool;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public interface Server_I {

    void configurate() throws IOException;

    void turn_off_server();

    void start() throws IOException;

    ThreadPool getSelectorsPool();

    void shutdown();

    ThreadFactory getSocketFactory();

    void setSocketFactory(ThreadFactory factory);

    ThreadPoolExecutor getThreadPoolExecutor();

    void setHost(String host);

    String getHost();

    void setThreadPool(ThreadPool pool_);

    void setPort(int port);

    int getPort();

    int getMaximumConnections();

    void setMaximumConnections(int count_connections);

    void setThreadPoolExecutor(ThreadPoolExecutor executor);

}
