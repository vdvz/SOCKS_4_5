package Server;

import java.util.concurrent.ThreadFactory;

public class SocketFactory implements ThreadFactory {
    Runnable runnable;

    SocketFactory(Runnable r_){
        runnable = r_;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(runnable);
    }


}
