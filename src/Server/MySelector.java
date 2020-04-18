package Server;

import Exceptions.End;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class MySelector implements Runnable{

    private boolean isLock = false;
    private boolean isOn = true;
    private Selector selector;
    private ByteBuffer buffer;

    MySelector(){
        buffer = ByteBuffer.allocate(4096);
        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public  void setLock(){
        isLock = true;
    }

    public void setUnlock(){
        isLock = false;
    }

    public Selector getSelector(){
        return selector;
    }

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
                if(!isLock) {

                    selector.select();

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
                                socks_task.setValidFLag(from);
                                try {
                                    Readable(from, to);
                                } catch (Exception ex) {
                                    System.out.println("set exception dlag ");
                                    key.cancel();
                                    socks_task.setCloseFLag(from);
                                    //socks_task.setClose();
                                }
                            }
                        }

                        if(key.isValid()) {
                            if (key.isAcceptable()) {
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

    public void Readable(SocketChannel in, SocketChannel out) throws Exception {
        int k;
        while((k = in.read(buffer))>0) {
            System.out.println("K: " + k);
            if(!buffer.hasRemaining()){
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
        }
        System.out.println("K: " + k);
        if(buffer.position()!=0){
            buffer.flip();
            out.write(buffer);
            buffer.clear();
        }
        if(k == -1){
            if(buffer.position()!=0){
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
            throw new End("End");
        }
    }

    public void Connectable(){
    }

    public void Acceptable(ThreadPoolExecutor threadPoolExecutor, ServerSocketChannel socket) throws IOException {
        System.out.println("accept now");
        threadPoolExecutor.execute(new Task(socket.accept(), 0));
    }

    public void shutdown(){
        isOn = false;
        Thread.currentThread().interrupt();
    }

}
