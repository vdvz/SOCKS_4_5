package Server;

import Exceptions.End;
import Exceptions.NoData;
import javafx.util.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Socks4 implements Socks_I {

    private SocketChannel client;
    private SocketChannel server;
    private ByteBuffer buffer;
    private MySelector client_selector = null;
    private MySelector server_selector = null;
    private boolean isClose = false;
    ReentrantLock lock = new ReentrantLock();
    Condition condition = lock.newCondition();

    public Socks4(SocketChannel client_, ByteBuffer buffer_) {
        client = client_;
        buffer = buffer_;
    }


    @Override
    public void parse() throws End {
        //start parsing socks4 request

        byte command = buffer.get();
        short port = buffer.getShort();
        byte []ip_v4 = new byte[4];
        buffer.get(ip_v4, 0, 4);
        ByteBuffer sub = ByteBuffer.allocate(256);
        byte b;
        while((b = buffer.get()) != (byte)0x00) sub.put(b);
        byte []ID = sub.array();
        if(request_to_BD(new String(ID), "socks4")){
            connect((byte) 0,ip_v4,null,null, port);
        }else{
            buffer.clear();
            buffer.put((byte)0x00).put((byte)0x5c).putInt(0).putShort((short)0);
            send(client);
            shutdown();
        }
    }



    @Override
    public void connect(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws End {

        buffer.clear();
        buffer.put((byte)0x00);
        try {
            server = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v4), port));
            buffer.put((byte)0x5a).putInt(0).putShort((short)0);
            send(client);
        } catch (IOException e) {
            buffer.put((byte)0x5b).putInt(0).putShort((short)0);
            send(client);
            shutdown();
        }


        try{
            client.configureBlocking(false);
            server.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //server_selector = Server.getInstance().getSelectorsPool().register(server, SelectionKey.OP_READ, new Pair<>(this, client));
        //client_selector = Server.getInstance().getSelectorsPool().register(client, SelectionKey.OP_READ, new Pair<>(this, server));

        lock.lock();
        try{
            while(!isClose){
                try {
                    condition.await();
                    close_sockets();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }finally {
            lock.unlock();
        }

    }

    @Override
    public void setCloseFLag(SocketChannel channel){
        lock.lock();
        //Уменьшает количество обрабатываемых селектором ключей в map в ThreadPool
        if(server_selector!=null) Server.getInstance().getSelectorsPool().unregister(server_selector);
        if(client_selector!=null) Server.getInstance().getSelectorsPool().unregister(client_selector);
        isClose = true;
        condition.signal();
        lock.unlock();
    }

    @Override
    public void setValidFLag(SocketChannel channel) {

    }

    @Override
    public byte authentication_method(byte[] methods) {
        return 0;
    }

    @Override
    public void receive(SocketChannel from) throws End {
        buffer.clear();
        try{
            int packet_length;
            if((packet_length = from.read(buffer))<0){
                shutdown();
            }
        } catch (IOException e) {
            shutdown();
            return;
        }
        //System.out.println("GET BYTES: " + packet_length);
        buffer.rewind();
    }

    @Override
    public void send(SocketChannel to) throws End {
        try {
            buffer.flip();
            System.out.println("SEND BYTES: " + to.write(buffer));
        } catch (IOException e) {
            //e.printStackTrace();
            shutdown();
        }
    }

    @Override
    public void close_sockets() {
        try {
            if(!client.socket().isClosed()) client.socket().close();
            if(!client.socket().isClosed() && server!=null) server.socket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean identification(SocketChannel client)  {
        return false;
    }

    @Override
    public boolean request_to_BD(String ID, String PW){
        return BD.getInstance().identByIdAndPw(ID, PW);
    }

    @Override
    public void shutdown() throws End {
        close_sockets();
        throw new End();
    }

}
