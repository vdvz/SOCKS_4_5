package Server;

import Exceptions.End;
import Exceptions.NoData;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Connection implements Runnable, Task {

    //client
    SocketChannel client;
    //server
    SocketChannel destination_Socket;
    //sending buffer
    ByteBuffer buffer;
    //timeout for selector
    long timeout = 10000;
    Selector selector;


    public Connection(SocketChannel client_) {
        buffer = ByteBuffer.allocate(4096);
        client = client_;
        try {
            client.configureBlocking(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //Receive first packet from client as it say at wiki
        try {
            receive_buffer(client);
            switch (buffer.get()) {
                case 0x04:
                    System.out.println("New connection!");
                    parse_request_SOCKS4();
                    break;
                case 0x05:
                    System.out.println("New connection!");
                    parse_request_SOCKS5();
                    break;
                default:
                    break;
            }

        }catch (End e){
            e.printStackTrace();
        }
    }

    @Override
    public void parse_request_SOCKS4() throws End {
        //start parsing socks4 request

        byte command = buffer.get();
        short port = buffer.getShort();
        byte []ip_v4 = new byte[4];
        buffer.get(ip_v4, 0, 4);
        ByteBuffer sub = ByteBuffer.allocate(256);
        byte b;
        while((b = buffer.get()) != (byte)0x00) sub.put(b);
        byte []ID = sub.array();
        if(ident_SOCKS4(ID)){
            connect_to_destinationServer_SOCKS4(ip_v4, port);
        }else{
            buffer.clear();
            buffer.put((byte)0x00).put((byte)0x5c).putInt(0).putShort((short)0);
            send_buffer(client);
            shutdown_task();
        }
    }

    @Override
    public void connect_to_destinationServer_SOCKS4(byte[] ip_v4, short port) throws End {
        //trying connect to destination server
        buffer.clear();
        buffer.put((byte)0x00);
        try {
            destination_Socket = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v4), port));
            buffer.put((byte)0x5a).putInt(0).putShort((short)0);
            send_buffer(client);
        } catch (IOException e) {
            buffer.put((byte)0x5b).putInt(0).putShort((short)0);
            send_buffer(client);
            shutdown_task();
            e.printStackTrace();
        }

        streaming();
        shutdown_task();

    }

    @Override
    public boolean ident_SOCKS4(byte[] id){
        //identification user if need it
        return request_to_BD(new String(id),null);
    }

    @Override
    public void receive_buffer(SocketChannel socket) throws End {
        //receiving buffer in block mode
        buffer.clear();
        int packet_length = 0;
        try {
            packet_length = socket.read(buffer);
        if(packet_length<=0){
            throw new NoData();
        }
        } catch (IOException | NoData e) {
            shutdown_task();
            e.printStackTrace();
        }
        System.out.println("GET BYTES: " + packet_length);
        buffer.rewind();
    }

    @Override
    public void send_buffer(SocketChannel socket) throws End {
        //sending buffer in block mode
        try {
            buffer.flip();
            System.out.println("SEND BYTES: " + socket.write(buffer));
        } catch (IOException e) {
            shutdown_task();
            e.printStackTrace();
        }
    }

    @Override
    public void streaming() throws End {
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

        while(true){
            try {
                //set up timeout if server or client dont answer in timeout then generate exception and thread interupt
                selector.select(timeout);
            } catch (Exception e) {
                shutdown_task();
                e.printStackTrace();
            }

            Set<SelectionKey> channels = selector.selectedKeys();
            Iterator<SelectionKey> iterator = channels.iterator();

            while(iterator.hasNext()){

                SelectionKey key = iterator.next();

                if(key.isReadable()){
                    if (key.channel() == client) {
                        try {

                            if (!(client.read(buffer)>0)) break;

                        } catch (IOException e) {
                            shutdown_task();
                            e.printStackTrace();
                        }
                        System.out.println("READ CLIENT: " + buffer.position());

                        buffer.flip();
                        try {
                            System.out.println("SEND SERVER: " + destination_Socket.write(buffer));
                        } catch (Exception e) {
                            shutdown_task();
                            e.printStackTrace();
                        }
                        buffer.clear();
                    } else {
                        try {
                            if (!(destination_Socket.read(buffer)>0)) break;
                        } catch (IOException e) {
                            shutdown_task();
                            e.printStackTrace();
                        }
                        System.out.println("READ SERVER: " + buffer.position());

                        buffer.flip();
                        try {
                            System.out.println("SEND CLIENT: " + client.write(buffer));
                        } catch (Exception e) {
                            shutdown_task();
                            e.printStackTrace();
                        }
                        buffer.clear();
                    }
                }

                iterator.remove();

            }
        }
    }

    @Override
    public void parse_request_SOCKS5() throws End {
        //first packet in socks5 protocol as it is on wiki
        System.out.println("Parsing socks 5");
        byte count_auth_methods = buffer.get();
        byte []auth_methods = new byte[count_auth_methods];
        buffer.get(auth_methods,0,count_auth_methods);
        byte picked_method = pick_auth_method_SOCKS5(auth_methods);
        buffer.clear();
        buffer.put((byte)0x05);
        buffer.put(picked_method);
        send_buffer(client);
        System.out.println("Send response for parsing socks5");

        //Mb will switch/case
        if(picked_method == 0x02){
            System.out.println("Authentification");
            if(!ident_SOCKS5(client)){
                buffer.clear();
                buffer.put((byte)0x01);
                buffer.put((byte)0x01);
                send_buffer(client);
                shutdown_task();
            }
            buffer.clear();
            buffer.put((byte)0x01);
            buffer.put((byte)0x00);
            send_buffer(client);
        }


        try{
            System.out.println("Get request");
            receive_buffer(client);

            if(buffer.get()!=0x05){
                throw new ProtocolException();
            }

            byte command = buffer.get();

            if(buffer.get()!=0x00){
                throw new ProtocolException();
            }

            byte ip_type = buffer.get();

            short port;

            switch(command) {
            case 0x01:
                //установка соединения TCP
                switch (ip_type){
                    case 0x01:
                        byte[] ip_v4 = new byte[4];
                        buffer.get(ip_v4, 0, 4);
                        port = buffer.getShort();
                        connect_to_destinationServer_SOCKS5(ip_type,ip_v4,null, null, port);
                        break;
                    case 0x03:
                        byte length = buffer.get();
                        byte[] host = new byte[length];
                        buffer.get(host,0, length);
                        port = buffer.getShort();
                        connect_to_destinationServer_SOCKS5(ip_type,null,null, new String(host), port);
                        break;
                    case 0x04:
                        byte []ip_v6 = new byte[16];
                        buffer.get(ip_v6,0,16);
                        port = buffer.getShort();
                        connect_to_destinationServer_SOCKS5(ip_type,null,ip_v6,null, port);
                        break;
                    default:
                        throw new ProtocolException();
                }
                break;
            case 0x02:
                //binding
                break;
            case 0x03:
                //установка UDP
            default:
                throw new ProtocolException();
            }
        }catch(ProtocolException ex){
            System.out.println("wrong command or protocol error");
            buffer.put(1,(byte)0x07);
            send_buffer(client);
            shutdown_task();
            ex.printStackTrace();
        }
    }

    @Override
    public boolean request_to_BD(String ID, String PW){
        return BD.getInstance().identByIdAndPw(ID, PW);
    }

    @Override
    public boolean ident_SOCKS5(SocketChannel client) throws End {

        receive_buffer(client);
        buffer.get();//it need for telegram but not say at wiki
        byte id_length = buffer.get();
        byte []ID = new byte[id_length];
        buffer.get(ID,0, id_length);
        byte pw_length = buffer.get();
        byte []PW = new byte[pw_length];
        buffer.get(PW, 0, pw_length);

        //System.out.println("ID: " + new String(ID) + " PW: " + new String(PW));

        return request_to_BD(new String(ID).trim(), new String(PW).trim());

    }

    @Override
    public void connect_to_destinationServer_SOCKS5(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws End {
        buffer.clear();
        buffer.put((byte)0x05);
        try {
            switch (ip_type) {
                case 0x01:
                    System.out.println("Try connect to destination");
                    destination_Socket = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v4), port));
                    System.out.println(destination_Socket.isConnected());
                    if(destination_Socket.isConnected()){
                        buffer.put((byte) 0x00).put((byte) 0x00).put(ip_type).put(ip_v4).putShort(port);
                        System.out.println("Open connection!");
                        send_buffer(client);
                    } else shutdown_task();
                    break;
                case 0x03:
                    destination_Socket = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(host), port));
                    if(destination_Socket.isConnected()){
                        buffer.put((byte) 0x00).put((byte) 0x00).put(ip_type).put((byte) host.getBytes().length).put(host.getBytes()).putShort(port);
                        System.out.println("Open connection!");
                        send_buffer(client);
                    } else shutdown_task();
                    break;
                case 0x04:
                    destination_Socket = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v6), port));
                    if(destination_Socket.isConnected()){
                        buffer.put((byte) 0x00).put((byte) 0x00).put(ip_type).put(ip_v6).putShort(port);
                        System.out.println("Open connection!");
                        send_buffer(client);
                    } else shutdown_task();
                    break;
                default:
                    shutdown_task();
                    break;
            }
        } /*catch(ClosedByInterruptException e) {
            System.out.println("If another thread interrupts the current thread while the connect operation is in progress,"
                    +" thereby closing the channel and setting the current thread's interrupt status");
            e.printStackTrace();
            shutdown_task();
        } catch(AsynchronousCloseException e) {
            System.out.println("If another thread closes this channel while the connect operation is in progress");
            e.printStackTrace();
            shutdown_task();
        }
        catch(UnresolvedAddressException e) {
            System.out.println("If the given remote address is not fully resolved");
            e.printStackTrace();
            shutdown_task();
        }
        catch(UnsupportedAddressTypeException e) {
            System.out.println("If the type of the given remote address is not supported");
            e.printStackTrace();
            shutdown_task();
        }
        catch(SecurityException e) {
            System.out.println("If a security manager has been installed and it does not permit access to the given remote endpoint");
            e.printStackTrace();
            shutdown_task();
        } catch(UnknownHostException e) {
            System.out.println("illegal length of ip");
            e.printStackTrace();
            shutdown_task();
        }
        catch(ConnectException e) {
            System.out.println("Signals that an error occurred while attempting to connect a socket to a remote address and port."+
                    " Typically, the connection was refused remotely");
            e.printStackTrace();
            shutdown_task();
        }*/
        catch(IOException e) {
            switch (ip_type) {
                case 0x01:
                    buffer.put((byte) 0x05).put((byte) 0x00).put(ip_type).put(ip_v4).putShort(port);
                    send_buffer(client);
                    break;
                case 0x03:
                    buffer.put((byte) 0x05).put((byte) 0x00).put(ip_type).put((byte) host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client);
                    break;
                case 0x04:
                    buffer.put((byte) 0x05).put((byte) 0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client);
                    break;
                default:
                    shutdown_task();
                    break;
            }
            shutdown_task();
            e.printStackTrace();

        }
        catch(Exception e){
            shutdown_task();
            e.printStackTrace();
        }

        streaming();

    }

    @Override
    public byte pick_auth_method_SOCKS5(byte[] methods){
        for (byte method : methods) {
            //System.out.println("MET: " + method);
            if (method == 0x02) return method;
        }
        return 0x00;
    }

    @Override
    public void shutdown_task() throws End {
        try {
            if(destination_Socket!=null){
                destination_Socket.shutdownInput();
                destination_Socket.shutdownOutput();
                destination_Socket.socket().close();
            }

            client.shutdownInput();
            client.shutdownOutput();
            client.socket().close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new End();
    }

}
