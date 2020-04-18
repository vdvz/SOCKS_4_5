package Server;

import Exceptions.End;
import Exceptions.NoData;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Task implements Runnable, Task_I {

    private SocketChannel client;
    private ByteBuffer buffer;
    int num;

    public Task(SocketChannel client_, int num_) {
        num = num_;
        System.out.println("NUM:" + num);
        buffer = ByteBuffer.allocate(4096);
        client = client_;
        try {
            client.configureBlocking(true);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("Hi");
        try {
            receive(client);
        } catch (End e){
            shutdown_task();
        }

        try {
            switch (buffer.get()) {
                case 0x04:
                    new Socks4(client, buffer).parse();
                    System.out.println("Bye");
                    break;
                case 0x05:
                    new Socks5(client, buffer, num).parse();
                    System.out.println("Bye");
                    break;
                default:
                    shutdown_task();
                    break;
            }
        }catch(End e){
            System.out.println("Bye");
            System.out.println("interrupt by end, cause is in Socks-class");
            e.printStackTrace();
        }
    }


    @Override
    public void receive(SocketChannel from) throws End {
        buffer.clear();
        try{
            int packet_length;
            if((packet_length = from.read(buffer))<0){
                throw new End();
            }
        } catch (IOException e) {
            throw new End();
        }
        //System.out.println("GET BYTES: " + packet_length);
        buffer.rewind();
    }

    @Override
    public void shutdown_task(){
        System.out.println("interrupt by shutdown, cause in Task-class");
        System.out.println("Bye");
        try {
            client.socket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
