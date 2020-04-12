import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.Arrays;

public class Connection implements Runnable, Task {

    Socket client = null;
    int packet_length;
    Socket destination_Socket = null;
    DatagramSocket client_U = null;
    DatagramSocket destination_Socket_U = null;
    ByteBuffer buffer;
    DatagramSocket client_UDP = null;
    int timeout = 5000;
    DatagramPacket datagramPacket;
    String name;

    public Connection(Socket client_, String name_) {
        name = name_;
        client = client_;
        try {
            client.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.out.println("error in protocol");
            //e.printStackTrace();
        }
    }

    public Connection(Socket client_, int timeout_) {
        timeout = timeout_;
        client = client_;
        try {
            client.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.out.println("error in protocol");
            //e.printStackTrace();
        }
    }

    @Override
    public void run() {
        buffer = ByteBuffer.allocate(1);
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

        buffer = ByteBuffer.allocate(1024);

        try {
            receive_buffer(client);
        } catch (NoData| SocketTimeoutException noData) {
            System.out.println("No getting data from first request by client");
            shutdown_task();
        }

        byte command = buffer.get();
        short port = buffer.getShort();
        byte []ip_v4 = new byte[4];
        buffer.get(ip_v4,0,4);
        byte []ID = new byte[packet_length - 7];
        buffer.get(ID, 0, packet_length - 8);
        if(ident_SOCKS4(ID)){
            connect_to_destinationServer_SOCKS4(ip_v4, port);
        }else{
            buffer = ByteBuffer.allocate(8);
            buffer.put((byte)0x00).put((byte)0x5c);
            send_buffer(client, 8);
        }

    }

    @Override
    public void exceptionsSOCKS4(Exception e) throws IOException, End {
        if(e instanceof SocketException){
            System.out.println("error when set up timeout");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof SecurityException){
            System.out.println("Close connection by security method");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof IllegalArgumentException){
            System.out.println("Port is illegal, should be between 0 and 65535");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof NullPointerException){
            System.out.println("Address is null");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof UnknownHostException){
            System.out.println("ip address or host could not be identify, illegal length of ip");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        }
        if(e instanceof IOException ){
            System.out.println("IO exception with destination server socket");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        }
    }

    @Override
    public void connect_to_destinationServer_SOCKS4(byte[] ip_v4, short port) throws IOException, End {
        buffer = ByteBuffer.allocate(8);
        buffer.rewind();
        buffer.put((byte)0x00);
        try {
            destination_Socket = new Socket(InetAddress.getByAddress(ip_v4), port);
            destination_Socket.setSoTimeout(timeout);
            buffer.put((byte)0x5a);
            send_buffer(client, 8);
        } catch(Exception ex){
            exceptionsSOCKS4(ex);
        }

        buffer = ByteBuffer.allocate(4096);
        try {

            shutdown_task();
        } catch (IOException e) {
            System.out.println("Connection reset");
            shutdown_task();
            e.printStackTrace();
        }
    }

    @Override
    public boolean ident_SOCKS4(byte[] id){
        return request_to_BD(new String(id),null);
    }

    @Override
    public void receive_buffer(Socket socket) throws IOException, NoData, End {
        //Если соединение есть, но данные не приходят будет брошено NoData
        //если с сокетом ассоциирован неблокирующий канал, то getinputstream кинет IllegalBlockingModeException.
        //если соединение было разорвано, то к потоку применяется:
        //1.Байты которые не были сброшены сетевым железом будут прочитаны
        //2.Если байтов больше нет то будет брошен IOException
        //3.Если байтов нет, а сокет не был закрыт то метод available вернет 0
        try{
            buffer.rewind();
            packet_length = socket.getInputStream().read(buffer.array());
            System.out.println("GET BYTES: " + packet_length);
            if(packet_length<=0){
                shutdown_task();
                throw new NoData();
            }
            buffer.rewind();
        }catch(SocketTimeoutException ex){
            System.out.println("WOW: " + destination_Socket.getInputStream().read(buffer.array()));
            System.out.println("WOW: " + client.getInputStream().read(buffer.array()));
            shutdown_task();
            ex.printStackTrace();
        }

    }

    @Override
    public void send_buffer(Socket socket, int length) throws IOException, End {
        try{
            buffer.rewind();
            System.out.println("SEND BYTES: " + length);
            socket.getOutputStream().write(buffer.array(), 0, length);
            socket.getOutputStream().flush();
            buffer.rewind();
        }catch(SocketTimeoutException ex){
            shutdown_task();
            ex.printStackTrace();
        }
    }

    @Override
    public void streaming() throws IOException, NoData, End {

        while(client.isConnected() && destination_Socket.isConnected()){
            try {
                streaming();
            } catch (NoData ignored){
            }

        }

        receive_buffer(client);

        send_buffer(destination_Socket, packet_length);

        receive_buffer(destination_Socket);

        send_buffer(client, packet_length);

    }

    @Override
    public void exceptionsSOCKS5(Exception e, byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws IOException, End {

        if(e instanceof BindException){
            System.out.println("Attempting bind a socket to local address and port wich is in use or couldn't be assigned");
            buffer.put((byte)0x04);
            switch (ip_type){
                case 0x01:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v4).putShort(port);
                    send_buffer(client, 10);
                    break;
                case 0x03:
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client, 7+host.getBytes().length);
                    break;
                case 0x04:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client, 22);
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
                    send_buffer(client, 10);
                    break;
                case 0x03:
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client, 7+host.getBytes().length);
                    break;
                case 0x04:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client, 22);
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
                    send_buffer(client, 10);
                    break;
                case 0x03:
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client, 7+host.getBytes().length);
                    break;
                case 0x04:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client, 22);
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
                    send_buffer(client, 10);
                    break;
                case 0x03:
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client, 7+host.getBytes().length);
                    break;
                case 0x04:
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client, 22);
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

    public void connect_to_destinationServer_UDP(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws IOException, End {

        buffer = ByteBuffer.allocate(262);
        buffer.rewind();
        buffer.put((byte)0x05);
        try {
            switch (ip_type){
                case 0x01:
                    destination_Socket_U = new DatagramSocket(port, InetAddress.getByAddress(ip_v4));
                    destination_Socket_U.setSoTimeout(timeout);
                    buffer.put((byte)0x00).put(ip_type).put(ip_v4).putShort(port);
                    send_buffer(client, 10);
                    break;
                case 0x03:
                    destination_Socket_U = new DatagramSocket(port, InetAddress.getByName(host));
                    destination_Socket_U.setSoTimeout(timeout);
                    buffer.put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                    send_buffer(client, 7 + host.getBytes().length);
                    break;
                case 0x04:
                    destination_Socket_U = new DatagramSocket(port, InetAddress.getByAddress(ip_v6));
                    destination_Socket_U.setSoTimeout(timeout);
                    buffer.put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                    send_buffer(client, 22);
                    break;
                default:
                    shutdown_task();
                    break;
            }
        } catch (Exception e){
            //e.printStackTrace();
        }

        try {
            client_U = new DatagramSocket();
            client_U.bind(client.getLocalSocketAddress());
        } catch (SocketException e) {
            System.out.println("Can't bind");
            e.printStackTrace();
        }
        buffer = ByteBuffer.allocate(4096);
        datagramPacket = new DatagramPacket(buffer.array(),buffer.capacity());
        while(true){
            try {
                streaming_UDP(client_U, destination_Socket_U);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void streaming_UDP(DatagramSocket client, DatagramSocket server) throws IOException {

        client.receive(datagramPacket);
        datagramPacket.setAddress(server.getInetAddress());
        datagramPacket.setPort(server.getPort());
        server.send(datagramPacket);

        server.receive(datagramPacket);
        datagramPacket.setAddress(client.getInetAddress());
        datagramPacket.setPort(client.getPort());
        client.send(datagramPacket);

    }

    @Override
    public void parse_request_SOCKS5() throws IOException, End {
        buffer = ByteBuffer.allocate(256);

        try {
            receive_buffer(client);
        } catch (NoData | SocketTimeoutException noData) {
            shutdown_task();
            noData.printStackTrace();
        }

        byte count_auth_methods = buffer.get();
        byte []auth_methods = new byte[count_auth_methods];
        buffer.get(auth_methods,1,count_auth_methods-1);
        byte picked_method = pick_auth_method_SOCKS5(auth_methods);
        buffer = ByteBuffer.allocate(2);
        buffer.put((byte)0x05);
        buffer.put(picked_method);


        send_buffer(client, 2);


        //Mb will switch/case
        if(picked_method == 0x02){
            buffer.rewind();
            buffer.put((byte)0x01);
            if(!ident_SOCKS5(client)){
                buffer.put((byte)0x01);
                send_buffer(client, 2);
                shutdown_task();
            }
            buffer.put((byte)0x00);
            send_buffer(client, 2);
        }


        try{

            buffer = ByteBuffer.allocate(262);

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
                switch (ip_type){
                    case 0x01:
                        byte[] ip_v4 = new byte[4];
                        buffer.get(ip_v4, 0, 4);
                        port = buffer.getShort();
                        connect_to_destinationServer_UDP(ip_type,ip_v4,null, null, port);
                        break;
                    case 0x03:
                        byte length = buffer.get();
                        byte[] host = new byte[length];
                        buffer.get(host,0, length);
                        port = buffer.getShort();
                        connect_to_destinationServer_UDP(ip_type,null,null, new String(host), port);
                        break;
                    case 0x04:
                        byte []ip_v6 = new byte[16];
                        buffer.get(ip_v6,0,16);
                        port = buffer.getShort();
                        connect_to_destinationServer_UDP(ip_type,null,ip_v6,null, port);
                        break;
                    default:
                        throw new ProtocolException();
                }
                break;
            default:
                throw new ProtocolException();
            }
        }catch(ProtocolException ex){
            System.out.println("wrong command or protocol error");
            buffer.put(1,(byte)0x07);
            send_buffer(client, packet_length);
            shutdown_task();
        }catch (Exception ex){
            exceptionsSOCKS5(ex, (byte)0x00, null, null, null, (short)0);
        }
    }

    @Override
    public boolean request_to_BD(String ID, String PW){
        return true;
    }

    @Override
    public boolean ident_SOCKS5(Socket client) throws IOException, End {
        buffer = ByteBuffer.allocate(515);

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
        buffer = ByteBuffer.allocate(262);
        buffer.rewind();
        buffer.put((byte)0x05);

        switch (ip_type){
            case 0x01:
                destination_Socket = new Socket(InetAddress.getByAddress(ip_v4), port);
                destination_Socket.setSoTimeout(timeout);
                buffer.put((byte)0x00).put((byte)0x00).put(ip_type).put(ip_v4).putShort(port);
                send_buffer(client, 10);
                break;
            case 0x03:
                destination_Socket = new Socket(host, port);
                destination_Socket.setSoTimeout(timeout);
                buffer.put((byte)0x00).put((byte)0x00).put(ip_type).put((byte)host.getBytes().length).put(host.getBytes()).putShort(port);
                send_buffer(client, 7+host.getBytes().length);
                break;
            case 0x04:
                destination_Socket = new Socket(InetAddress.getByAddress(ip_v6), port);
                destination_Socket.setSoTimeout(timeout);
                buffer.put((byte)0x00).put((byte)0x00).put(ip_type).put(ip_v6).putShort(port);
                send_buffer(client, 22);
                break;
            default:
                shutdown_task();
                break;
        }

        buffer = ByteBuffer.allocate(4096);
        try {
            while(client.isConnected() && destination_Socket.isConnected()){
                try {
                    streaming();
                } catch (NoData ignored){}
            }
            shutdown_task();
        } catch (IOException e) {
            System.out.println("Connection reset");
            e.printStackTrace();
            shutdown_task();
        }

    }

    @Override
    public byte pick_auth_method_SOCKS5(byte[] methods){
        for (byte method : methods) {
            if (method == 0x02) return method;
        }
        return 0x00;
    }

    @Override
    public void shutdown_task() throws IOException, End {

        if(destination_Socket!=null){
            destination_Socket.shutdownOutput();
            destination_Socket.shutdownInput();
            destination_Socket.close();
        }
        if(destination_Socket_U!=null){
            destination_Socket_U.close();
        }
        if(client_U!=null){
            client_U.close();
        }
        client.shutdownOutput();
        client.shutdownInput();
        client.close();
        throw new End();
    }


}
