package Server.Selectors;

import Server.Connection.Task;
import Server.Protocols.Socks_I;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class MySelector implements Runnable, MySelector_I{

    private volatile boolean isLock = false;
    private boolean isOn = true;
    private Selector selector;
    private ByteBuffer buffer;

    public MySelector(){
        buffer = ByteBuffer.allocate(4096);
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setBuffer(ByteBuffer buffer_){
        buffer = buffer_;
    }

    @Override
    public ByteBuffer getBuffer(){
        return buffer;
    }

    @Override
    public  void setLock(){
        isLock = true;
    }

    @Override
    public void setUnlock(){
        isLock = false;
    }

    @Override
    public Selector getSelector(){
        return selector;
    }

    @Override
    public synchronized SelectionKey register(SocketChannel channel, int ops, Object attach){
        try {
            setLock();
            selector.wakeup();
            SelectionKey key = channel.register(selector, ops, attach);
            setUnlock();
            return key;
        } catch (ClosedChannelException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void register(ServerSocketChannel channel, int ops, Object attach){
        try {
            setLock();
            selector.wakeup();
            channel.register(selector, ops, attach);
            setUnlock();
        } catch (ClosedChannelException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while(isOn){
                //System.out.println("Lock: " + isLock);
                if(!isLock) {
                    selector.select();
                    //System.out.println("select size: " + selector.select());
                    Set<SelectionKey> channels = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = channels.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if(key.isValid()) {

                            if (key.isReadable()) {
                                Pair<Socks_I, SocketChannel> pair = (Pair<Socks_I, SocketChannel>) key.attachment();
                                SocketChannel to = pair.getValue();
                                SocketChannel from = (SocketChannel) key.channel();
                                Socks_I socks_task = pair.getKey();
                                try {
                                    Readable(from, to, socks_task);
                                } catch (IOException ex) {
                                    key.cancel();
                                    socks_task.setCloseFLag();
                                    //ex.printStackTrace();
                                }
                            }
                        }
                        if(key.isValid()){
                            if (key.isAcceptable() && !key.isReadable()) {
                                System.out.println("Accept");
                                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) key.attachment();
                                Acceptable(threadPoolExecutor, (ServerSocketChannel) key.channel());
                            }


                        }

                        iterator.remove();

                    }

                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void Readable(SocketChannel in, SocketChannel out, Socks_I socks_task) throws IOException {
        int send = -1;
        try {
            while(in.read(buffer)>0){
                if(!buffer.hasRemaining()){
                    buffer.flip();
                    send+=out.write(buffer);
                    buffer.clear();
                }
            }
            if(buffer.position()!=0){
                buffer.flip();
                send+=out.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            in.socket().close();
            out.socket().close();
            throw new IOException();
        }
        if(send != -1){
            socks_task.setValidFLag();
        }
        //System.out.println("Send: " + send + " Thread: " + socks_task.getThread().getName());

    }

    @Override
    public void Connectable(){
    }

    @Override
    public void Acceptable(ThreadPoolExecutor threadPoolExecutor, ServerSocketChannel serverSocket) throws IOException {

        if(threadPoolExecutor.getQueue().size()>1){
            threadPoolExecutor.getQueue().forEach(threadPoolExecutor::remove);
        }

        SocketChannel socket = serverSocket.accept();
        socket.socket().setKeepAlive(true);
        threadPoolExecutor.execute(new Task(socket));

    }

    @Override
    public void shutdown(){
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isOn = false;
        Thread.currentThread().interrupt();
    }


}
