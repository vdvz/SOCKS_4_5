import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public interface Server_I {

    void configurate() throws IOException;

    void start() throws IOException;

    void shutdown();

    ThreadFactory getSocketFactory();

    void setSocketFactory(ThreadFactory factory);

    ThreadPoolExecutor getThreadPoolExecutor();

    void setPort(int port);

    int getPort();

    int getMaximumConnections();

    void setMaximumConnections(int count_connections);

    void setThreadPoolExecutor(ThreadPoolExecutor executor);



}
