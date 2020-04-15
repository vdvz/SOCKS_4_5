import Exceptions.End;
import Exceptions.NoData;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class SubTest {

    Socket client;
    Socket destination_Socket;
    ByteBuffer buffer;
    int packet_length;
    int timeout;
    public void shutdown_task(){}

    public void parse_request_SOCKS4() throws IOException, End {
        buffer = ByteBuffer.allocate(7);

        try {
            receive_buffer(client);
        } catch (NoData noData) {
            System.out.println("No getting data from first request by client");
            shutdown_task();
        }

        byte command = buffer.get();
        short port = buffer.getShort();
        byte []ip_v4 = new byte[4];
        buffer.get(ip_v4,0,4);

        connect_to_destinationServer_SOCKS4(ip_v4, port);

    }

    public void connect_to_destinationServer_SOCKS4(byte[] ip_v4, short port) throws IOException, End {
        buffer = ByteBuffer.allocate(8);
        buffer.rewind();
        buffer.put((byte)0x00);
        try {
            destination_Socket = new Socket(InetAddress.getByAddress(ip_v4), port);
            destination_Socket.setSoTimeout(timeout);
            buffer.put((byte)0x5a);
            send_buffer(client, 8);
        } catch (SocketException e){
            System.out.println("error when set up timeout");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        } catch (SecurityException e){
            System.out.println("Close connection by security method");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        } catch (IllegalArgumentException e){
            System.out.println("Port is illegal, should be between 0 and 65535");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        } catch (NullPointerException e){
            System.out.println("Address is null");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        } catch (UnknownHostException e){
            System.out.println("ip address or host could not be identify, illegal length of ip");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IO exception with destination server socket");
            buffer.put((byte)0x5b);
            send_buffer(client, 8);
            shutdown_task();
            e.printStackTrace();
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
            System.out.println("Server.Connection reset");
            shutdown_task();
            e.printStackTrace();
        }
    }

    public void receive_buffer(Socket socket) throws IOException, NoData {
        //Если соединение есть, но данные не приходят будет брошено Exceptions.NoData
        //если с сокетом ассоциирован неблокирующий канал, то getinputstream кинет IllegalBlockingModeException.
        //если соединение было разорвано, то к потоку применяется:
        //1.Байты которые не были сброшены сетевым железом будут прочитаны
        //2.Если байтов больше нет то будет брошен IOException
        //3.Если байтов нет, а сокет не был закрыт то метод available вернет 0
        buffer.rewind();
        packet_length = socket.getInputStream().read(buffer.array());
        if(packet_length<=0){
            throw new NoData();
        }
        buffer.rewind();
    }

    public void send_buffer(Socket socket, int length) throws IOException {
        System.out.println("BUFER SIZE: " + buffer.array().length);
        //System.out.println("VALUE: " + buffer.getInt());
        buffer.rewind();
        socket.getOutputStream().write(buffer.array(), 0, length);
        socket.getOutputStream().flush();
        buffer.rewind();
    }

    public void streaming() throws IOException, NoData {

        receive_buffer(client);

        send_buffer(destination_Socket, packet_length);

        receive_buffer(destination_Socket);

        send_buffer(destination_Socket, packet_length);

    }



}
