import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class Connection implements Runnable, Task {

    SocketChannel client;
    int packet_length;
    SocketChannel destination_Socket;
    ByteBuffer buffer;
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
        try {
            try {
                receive_buffer(client);
                switch (buffer.get()) {
                    case 0x04:
                        parse_request_SOCKS4();
                        break;
                    case 0x05:
                        parse_request_SOCKS5();
                        break;
                    default:
                        break;
                }
            }catch(NoData e){
                shutdown_task();
                e.printStackTrace();
            }
        }catch (End ignore){

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void parse_request_SOCKS4() throws IOException, End {

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
            buffer.put((byte)0x00).put((byte)0x5c);
            send_buffer(client);
        }
    }

    @Override
    public void exceptionsSOCKS4(Exception e) throws IOException, End {
        if(e instanceof SocketException){
            System.out.println("error when set up timeout");
            buffer.put((byte)0x5b);
            send_buffer(client);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof SecurityException){
            System.out.println("Close connection by security method");
            buffer.put((byte)0x5b);
            send_buffer(client);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof IllegalArgumentException){
            System.out.println("Port is illegal, should be between 0 and 65535");
            buffer.put((byte)0x5b);
            send_buffer(client);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof NullPointerException){
            System.out.println("Address is null");
            buffer.put((byte)0x5b);
            send_buffer(client);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof UnknownHostException){
            System.out.println("ip address or host could not be identify, illegal length of ip");
            buffer.put((byte)0x5b);
            send_buffer(client);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof IOException ){
            System.out.println("IO exception with destination server socket");
            buffer.put((byte)0x5b);
            send_buffer(client);
            shutdown_task();
            e.printStackTrace();
        }
    }

    @Override
    public void connect_to_destinationServer_SOCKS4(byte[] ip_v4, short port) throws IOException, End {
        buffer.clear();
        buffer.put((byte)0x00);
        try {
            destination_Socket = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v4), port));
            buffer.put((byte)0x5a);
            send_buffer(client);
        } catch(Exception ex){
            exceptionsSOCKS4(ex);
        }

        shutdown_task();
        try {
            streaming();
            shutdown_task();
        } catch (IOException | NoData e) {
            System.out.println("Connection reset");
            shutdown_task();
            e.printStackTrace();
        }
    }

    @Override
    public boolean ident_SOCKS4(byte[] id){
        return request_to_BD(new String(id),null);
    }

    public void receive_buffer(SocketChannel socket) throws SocketTimeoutException, IOException, NoData {
        //Если соединение есть, но данные не приходят будет брошено NoData
        //если с сокетом ассоциирован неблокирующий канал, то getinputstream кинет IllegalBlockingModeException.
        //если соединение было разорвано, то к потоку применяется:
        //1.Байты которые не были сброшены сетевым железом будут прочитаны
        //2.Если байтов больше нет то будет брошен IOException
        //3.Если байтов нет, а сокет не был закрыт то метод available вернет 0
        buffer.clear();
        packet_length = socket.read(buffer);
        if(packet_length<=0){
            throw new NoData();
        }
        System.out.println("GET BYTES: " + packet_length);
        buffer.rewind();
    }

    public void send_buffer(SocketChannel socket) throws IOException, End {
        buffer.flip();
        System.out.println("SEND BYTES: " + socket.write(buffer));
    }

    @Override
    public void streaming() throws IOException, End, NoData {

        System.out.println("HERE");
        selector = Selector.open();

        client.configureBlocking(false);
        destination_Socket.configureBlocking(false);

        SelectionKey client_key = client.register(selector, SelectionKey.OP_WRITE);
        SelectionKey client_key_1 = client.register(selector, SelectionKey.OP_READ);
        SelectionKey server_key = destination_Socket.register(selector, SelectionKey.OP_READ);
        SelectionKey server_key_1 = destination_Socket.register(selector,  SelectionKey.OP_WRITE);

        //System.out.println("HERE");

        ByteBuffer sub_buf = ByteBuffer.allocate(4096);


        while(true){
            System.out.println("HERE");
            selector.select();

            Set<SelectionKey> channels = selector.selectedKeys();
            Iterator<SelectionKey> iterator = channels.iterator();

            for (SelectionKey key : channels) {
                if(key.isWritable()){
                    if (key.channel() == client) {
                        sub_buf.flip();
                        System.out.println("SEND BYTES_C: " + client.write(sub_buf));
                    } else {
                        send_buffer(destination_Socket);
                    }
                }

                if (key.isReadable()) {
                    if (key.channel() == client) {
                        receive_buffer(client);
                    } else {
                        sub_buf.clear();
                        packet_length = destination_Socket.read(sub_buf);
                        sub_buf.rewind();
                    }
                }


                iterator.remove();
            }


        }
    }

    @Override
    public void exceptionsSOCKS5(Exception e, byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws IOException, End {

        if(e instanceof BindException){
            System.out.println("Attempting bind a socket to local address and port wich is in use or couldn't be assigned");
            buffer.put((byte)0x04);
            switch (ip_type){
                case 0x01:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v4).putShort(port);
                    send_buffer(client);
                    break;
                case 0x03:
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client);
                    break;
                case 0x04:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client);
                    break;
                default:
                    break;
            }
            shutdown_task();
            return;
        }
        if(e instanceof ConnectException){
            System.out.println("Connection was refused remotely");
            buffer.put((byte)0x05);
            switch (ip_type){
                case 0x01:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v4).putShort(port);
                    send_buffer(client);
                    break;
                case 0x03:
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client);
                    break;
                case 0x04:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client);
                    break;
                default:
                    break;
            }
            shutdown_task();
            return;
        }
        if(e instanceof NoRouteToHostException){
            System.out.println("Can't connect to remote address, reason tipically is the host block by firewall");
            buffer.put((byte)0x02);
            switch (ip_type){
                case 0x01:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v4).putShort(port);
                    send_buffer(client);
                    break;
                case 0x03:
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client);
                    break;
                case 0x04:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client);
                    break;
                default:
                    break;
            }
            shutdown_task();
            return;
        }
        if(e instanceof PortUnreachableException){
            System.out.println("Receive on a connected datagramm");
            buffer.put((byte)0x04);
            switch (ip_type){
                case 0x01:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v4).putShort(port);
                    send_buffer(client);
                    break;
                case 0x03:
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client);
                    break;
                case 0x04:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client);
                    break;
                default:
                    break;
            }
            shutdown_task();
            return;
        }
        if(e instanceof UnknownHostException){

            return;
        }
        if(e instanceof ProtocolException){
            throw new ProtocolException();
        }
        if(e instanceof InterruptedByTimeoutException){
            System.out.println("no answer from server");
            return;
        }
        if(e instanceof SocketTimeoutException){
            return;
        }
        if(e instanceof SecurityException){
            System.out.println("Close connection by security method");
            return;
        }
        if(e instanceof IllegalArgumentException){
            System.out.println("Port is illegal, should be between 0 and 65535");
            return;
        }
        if(e instanceof NullPointerException){
            System.out.println("Port is illegal, should be between 0 and 65535");
            return;
        }
        if(e instanceof IOException){

            return;
        }
        e.printStackTrace();
        shutdown_task();

    }

    @Override
    public void parse_request_SOCKS5() throws IOException, End {

        byte count_auth_methods = buffer.get();
        byte []auth_methods = new byte[count_auth_methods];
        buffer.get(auth_methods,0,count_auth_methods);
        byte picked_method = pick_auth_method_SOCKS5(auth_methods);
        buffer.clear();
        buffer.put((byte)0x05);
        buffer.put(picked_method);
        send_buffer(client);


        //Mb will switch/case
        if(picked_method == 0x02){
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
            try {
                receive_buffer(client);
            } catch (NoData noData) {
                shutdown_task();
                noData.printStackTrace();
            }

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
        }catch (Exception ex){
            exceptionsSOCKS5(ex, (byte)0x00, null, null, null, (short)0);
        }
    }

    @Override
    public boolean request_to_BD(String ID, String PW){
        System.out.println("Connect!");
        return true;
    }

    public boolean ident_SOCKS5(SocketChannel client) throws IOException, End {

        try {
            receive_buffer(client);
        } catch (NoData noData) {
            shutdown_task();
            noData.printStackTrace();
        }

        byte id_length = buffer.get();
        byte []ID = new byte[id_length];
        buffer.get(ID,0, id_length);
        byte pw_length = buffer.get();
        byte []PW = new byte[pw_length];
        buffer.get(PW, 0, pw_length);

        return request_to_BD(new String(ID), new String(PW));

    }

    @Override
    public void connect_to_destinationServer_SOCKS5(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws ProtocolException, IOException, End {
        buffer.clear();
        buffer.put((byte)0x05);

        switch (ip_type){
            case 0x01:
                System.out.println("HERE3: " + port + " : " + InetAddress.getByAddress(ip_v4).getHostAddress());
                destination_Socket = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v4), port));
                System.out.println("HERE4");
                buffer.put((byte)0x00).put((byte)0x00).put(ip_type).put(ip_v4).putShort(port);
                System.out.println("HERE5");
                send_buffer(client);
                break;
            case 0x03:
                destination_Socket = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(host), port));
                buffer.put((byte)0x00).put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                send_buffer(client);
                break;
            case 0x04:
                destination_Socket = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v6), port));
                buffer.put((byte)0x00).put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                send_buffer(client);
                break;
            default:
                System.out.println("Here!");
                //shutdown_task();
                break;
        }
        System.out.println("HERE1");
        try {
            streaming();
            System.out.println("HERE2");
        } catch (IOException | NoData e) {
            System.out.println("Connection reset");
            e.printStackTrace();
            shutdown_task();
        }

    }

    @Override
    public byte pick_auth_method_SOCKS5(byte[] methods){
        for (byte method : methods) {
            System.out.println("MET: " + method);
            if (method == 0x02) return method;
        }
        return 0x00;
    }

    @Override
    public void shutdown_task() throws IOException, End {

        if(destination_Socket!=null){
            destination_Socket.shutdownInput();
            destination_Socket.shutdownOutput();
            destination_Socket.socket().close();
        }

        client.shutdownInput();
        client.shutdownOutput();
        client.socket().close();

        throw new End();
    }


}
