package DestinationServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Test_Server implements Runnable {
    int DESTINATION_PORT = 81;

    @Override
    public void run() {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        try {
            ServerSocket serverSocket = new ServerSocket(DESTINATION_PORT);
            while(true){
                Socket client = serverSocket.accept();
                threadPoolExecutor.submit(new DestinationServer(client));
            }
        } catch (IOException e) {
            System.out.println("error");
            e.printStackTrace();
        }
    }

}
