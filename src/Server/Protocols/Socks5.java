package Server.Protocols;

import Exceptions.End;
import Server.BD.BD;
import Server.Selectors.MySelector;
import Server.Server;
import javafx.util.Pair;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Socks5 implements Socks_I {
    private Thread thread = Thread.currentThread();
    private int timeout = 100000;
    private MySelector client_selector = null;
    private MySelector server_selector = null;
    private SocketChannel client;
    private SocketChannel server = null;
    private ByteBuffer buffer;
    private volatile boolean isClose = false;

    ReentrantLock lock = new ReentrantLock();
    Condition condition = lock.newCondition();


    public Socks5(SocketChannel client_, ByteBuffer buffer_, int timeout_){
        client = client_;
        buffer = buffer_;
        timeout = timeout_;
    }

    public Socks5(SocketChannel client_, ByteBuffer buffer_){
        client = client_;
        buffer = buffer_;
    }


    @Override
    public Thread getThread() {
        return thread;
    }

    @Override
    public void parse() throws End {

        System.out.println("Parsing socks 5");
        byte count_auth_methods = buffer.get();
        byte []auth_methods = new byte[count_auth_methods];
        buffer.get(auth_methods,0,count_auth_methods);
        byte picked_method = authentication_method(auth_methods);
        buffer.clear();
        buffer.put((byte)0x05);
        buffer.put(picked_method);
        send(client);

        if(picked_method == 0x02){
            if(!identification(client)){
                buffer.clear();
                buffer.put((byte)0x01);
                buffer.put((byte)0x01);
                send(client);
                shutdown();
            }
            System.out.println("Ident!");
            buffer.clear();
            buffer.put((byte)0x01);
            buffer.put((byte)0x00);
            send(client);
        }


        try{
            receive(client);

            if(buffer.get()!=0x05){
                throw new ProtocolException();
            }

            byte command = buffer.get();

            if(buffer.get()!=0x00){
                throw new ProtocolException();
            }

            byte ip_type = buffer.get();

            short port;

            if (command == 0x01) {//установка соединения TCP
                switch (ip_type) {
                    case 0x01:
                        byte[] ip_v4 = new byte[4];
                        buffer.get(ip_v4, 0, 4);
                        port = buffer.getShort();
                        connect(ip_type, ip_v4, null, null, port);
                        break;
                    case 0x03:
                        byte length = buffer.get();
                        byte[] host = new byte[length];
                        buffer.get(host, 0, length);
                        port = buffer.getShort();
                        connect(ip_type, null, null, new String(host), port);
                        break;
                    case 0x04:
                        byte[] ip_v6 = new byte[16];
                        buffer.get(ip_v6, 0, 16);
                        port = buffer.getShort();
                        connect(ip_type, null, ip_v6, null, port);
                        break;
                    default:
                        throw new ProtocolException();
                }
            } else {
                throw new ProtocolException();
            }
        }catch(ProtocolException ex){
            System.out.println("wrong command or protocol error");
            buffer.put(1,(byte)0x07);
            send(client);
            shutdown();
        }
    }


    @Override
    public void setCloseFLag() {
        lock.lock();
        try {
            if(!isClose) {
                isClose = true;
                condition.signal();
                System.out.println("EXCEPTION IS SET: " + getThread().getName());
            }
        }finally{
            lock.unlock();
        }
    }


    @Override
    public void setValidFLag(){
        lock.lock();
        try {
            condition.signal();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void connect(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws End {
        buffer.clear();
        buffer.put((byte)0x05);
        try {
            switch (ip_type) {
                case 0x01:
                    server = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v4), port));
                    server.finishConnect();
                    if(server.isConnected()){
                        server.socket().setKeepAlive(true);
                        buffer.put((byte) 0x00).put((byte) 0x00).put(ip_type).put(ip_v4).putShort(port);
                        System.out.println("Open connection!");
                        send(client);
                    } else throw new End();
                    break;
                case 0x03:
                    server = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(host), port));
                    server.finishConnect();
                    if(server.isConnected()){
                        server.socket().setKeepAlive(true);
                        buffer.put((byte) 0x00).put((byte) 0x00).put(ip_type).put((byte) host.getBytes().length).put(host.getBytes()).putShort(port);
                        System.out.println("Open connection!");
                        send(client);
                    } else throw new End();
                    break;
                case 0x04:
                    server = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v6), port));
                    server.finishConnect();
                    if(server.isConnected()){
                        server.socket().setKeepAlive(true);
                        buffer.put((byte) 0x00).put((byte) 0x00).put(ip_type).put(ip_v6).putShort(port);
                        System.out.println("Open connection!");
                        send(client);
                    } else throw new End();
                    break;
                default:
                    shutdown();
            }
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            switch (ip_type) {
                case 0x01:
                    buffer.put((byte) 0x05).put((byte) 0x00).put(ip_type).put(ip_v4).putShort(port);
                    send(client);
                    break;
                case 0x03:
                    buffer.put((byte) 0x05).put((byte) 0x00).put(ip_type).put((byte) host.getBytes().length).put(host.getBytes()).putShort(port);
                    send(client);
                    break;
                case 0x04:
                    buffer.put((byte) 0x05).put((byte) 0x00).put(ip_type).put(ip_v6).putShort(port);
                    send(client);
                    break;
                default:
                    break;
            }
            shutdown();
        }


        System.out.println("Streaminig: " + Thread.currentThread().getName());

        try{
            client.configureBlocking(false);
            server.configureBlocking(false);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            shutdown();
        }


        server_selector = Server.getInstance().getSelectorsPool().register(server, SelectionKey.OP_READ, new Pair<>(this, client));
        client_selector = Server.getInstance().getSelectorsPool().register(client, SelectionKey.OP_READ, new Pair<>(this, server));

        lock.lock();
        try{
            while(!isClose){
                try {
                    if(!condition.await(timeout,TimeUnit.MILLISECONDS)) isClose = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Start exit: " + getThread().getName());
        }finally {
            lock.unlock();
        }
        System.out.println("Procced to close connect: " + getThread().getName());
        close_sockets();

    }

    @Override
    public byte authentication_method(byte[] methods) {
        for (byte method : methods) {
            //System.out.println("MET: " + method);
            if (method == 0x02) return method;
        }
        return 0x00;
    }

    @Override
    public void receive(SocketChannel from) throws End {
        buffer.clear();
        int packet_length = 0;
        try{
            packet_length = from.read(buffer);
            System.out.println("GET BYTES FROM CLIENT: " + packet_length);
        } catch (Exception e) {
            shutdown();
            e.printStackTrace();
        }

        if(packet_length<1){
            shutdown();
        }

        //System.out.println("GET BYTES: " + packet_length);
        buffer.rewind();
    }

    @Override
    public void send(SocketChannel to) throws End {
        try {
            buffer.flip();
            System.out.println("SEND BYTES TO SERVER: " + to.write(buffer));
        } catch (Exception e) {
            shutdown();
            e.printStackTrace();
        }
    }


    @Override
    public void close_sockets(){
        System.out.println("Close socket for thread: " + getThread().getName());
        try {
            if(server_selector!=null)Server.getInstance().getSelectorsPool().unregister(server_selector);
            if(client_selector!=null)Server.getInstance().getSelectorsPool().unregister(client_selector);
            if(!client.socket().isClosed()) client.socket().close();
            if(server!=null) if(!server.socket().isClosed()) server.socket().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean identification(SocketChannel client) throws End {
        receive(client);
        buffer.get();
        byte id_length = buffer.get();
        byte []ID = new byte[id_length];
        buffer.get(ID,0, id_length);
        byte pw_length = buffer.get();
        byte []PW = new byte[pw_length];
        buffer.get(PW, 0, pw_length);

        return request_to_BD(new String(ID).trim(), new String(PW).trim());

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
