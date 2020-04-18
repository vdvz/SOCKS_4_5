package Server;

import Exceptions.End;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Connection implements Connection_I, Runnable {

    SocketChannel client;
    SocketChannel server;
    ByteBuffer buffer;

    Connection(SocketChannel client_,SocketChannel server_,ByteBuffer buffer_){
        client = client_;
        server = server_;
        buffer = buffer_;

    }


    public void send_packet(SocketChannel in, SocketChannel out, ByteBuffer buffer) throws IOException {
            int k;
            while((k = in.read(buffer))>0) {
                System.out.println("K: " + k);
                if(!buffer.hasRemaining()){
                    buffer.flip();
                    System.out.println("Here send: " + out.write(buffer));
                    buffer.clear();
                }
            }
            if(buffer.position()!=0){
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
    }

    @Override
    public void run() {

        try {
            Selector selector = Selector.open();

            client.configureBlocking(false);
            server.configureBlocking(false);

            client.register(selector, SelectionKey.OP_READ);
            server.register(selector, SelectionKey.OP_READ);

            buffer.clear();
            while(true){

                int k = selector.select();
                Set<SelectionKey> channels = selector.selectedKeys();
                if (channels.size() == 0) throw new End();
                Iterator<SelectionKey> iterator = channels.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    if (key.isReadable()) {
                        if (key.channel() == client) {
                            send_packet(client, server, buffer);
                        }
                        if (key.channel() == server) {
                            send_packet(server, client, buffer);
                        }
                    }
                    iterator.remove();
                }
            }
    } catch (IOException | End e) {
            e.printStackTrace();
    }
    }
    /*public void streaming() throws End {
        //start streaming in non-block mode
        try {
            //set up selector and non-block mode
            selector = Selector.open();
            client.configureBlocking(false);
            destination_Socket.configureBlocking(false);
            //set up keys - reaction on reading from socket
            client.register(selector, SelectionKey.OP_READ);
            destination_Socket.register(selector, SelectionKey.OP_READ);

        } catch (IOException e) {
            shutdown_task();
            e.printStackTrace();
        }

        buffer.clear();
        ByteBuffer buffer_server = ByteBuffer.allocate(4096);
        buffer_server.clear();
        System.out.println("HERE");
        while(true){
            try {
                //set up timeout if server or client dont answer in timeout then generate exception and thread interupt
                selector.select();
            } catch (Exception e) {
                shutdown_task();
                e.printStackTrace();
            }


            Set<SelectionKey> channels = selector.selectedKeys();
            //System.out.println("here");
            if(channels.size()==0) shutdown_task();
            Iterator<SelectionKey> iterator = channels.iterator();

            while(iterator.hasNext()){
                SelectionKey key = iterator.next();

                if(key.isReadable()){
                    if (key.channel() == client) {
                        try {
                            int k;
                            while((k = client.read(buffer))!=-1) {
                                if(!buffer.hasRemaining()){
                                    buffer.flip();
                                    System.out.println("Here to server: " + destination_Socket.write(buffer));
                                    buffer.clear();
                                }
                            }
                            System.out.println("K: " + k);
                            if(buffer.position()!=0){
                                buffer.flip();
                                destination_Socket.write(buffer);
                                buffer.clear();
                            }
                        } catch (IOException e) {
                            shutdown_task();
                            e.printStackTrace();
                        }
                    }
                    if (key.channel() == destination_Socket) {
                        try {
                            int k;
                            while((k = destination_Socket.read(buffer_server))!=-1) {
                                if(!buffer_server.hasRemaining()){
                                    buffer_server.flip();
                                    System.out.println("Here to client: " + client.write(buffer_server));
                                    buffer_server.clear();
                                }
                            }
                            System.out.println("K: " + k);
                            if(buffer_server.position()!=0) {
                                buffer_server.flip();
                                client.write(buffer_server);
                                buffer_server.clear();
                            }
                        } catch (IOException e) {
                            shutdown_task();
                            e.printStackTrace();
                        }
                    }
                }

                iterator.remove();

            }
        }
    }


}

*/

}