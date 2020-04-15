package DestinationServer;

import Exceptions.NoData;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class DestinationServer implements Runnable {

    Socket client = null;
    ByteBuffer buffer;

    DestinationServer(Socket socket){
        client = socket;
        try {
            client.setSoTimeout(10000);
        } catch (SocketException e) {
            shutdown();
            e.printStackTrace();
        }
    }

    public void receive_buffer(Socket socket) throws NoData {
        buffer.rewind();
        int i = 0;
        try {
            i = socket.getInputStream().read(buffer.array());
            if(i<=0) throw new NoData();
        } catch (SocketTimeoutException e) {
            System.out.println("time is out");
            shutdown();
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        buffer.rewind();
        System.out.println("receive bytes: " + i);
    }

    public void send_buffer(Socket socket) throws IOException {
        buffer.rewind();
        socket.getOutputStream().write(buffer.array(), 0,4);
        socket.getOutputStream().flush();
        buffer.rewind();
    }

    @Override
    public void run() {
        buffer = ByteBuffer.allocate(500);
        try {
            while(true){
                try {
                    receive_buffer(client);
                } catch (NoData noData) {
                    continue;
                    //noData.printStackTrace();
                }
                System.out.println("Get int:" + buffer.getInt());
                buffer.rewind();
                buffer.putInt(90);
                System.out.println("Send int: " + 90);
                send_buffer(client);
            }
        }catch (SocketTimeoutException e){
            System.out.println("time is out");
            shutdown();
        }
        catch (IOException e) {
            shutdown();
            e.printStackTrace();
        }

    }

    public void shutdown() {
        if(client!=null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
