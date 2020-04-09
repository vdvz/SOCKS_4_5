import com.sun.deploy.util.ArrayUtil;
import sun.nio.cs.UTF_32BE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

public class Connection implements Runnable, Task {

    Socket client;
    Socket destination_Socket;
    ByteBuffer buffer;

    public Connection(Socket client_) {
        client = client_;
    }

    @Override
    public void run() {
        buffer = ByteBuffer.allocate(1);
        try {
            receive_buffer(client);
            switch(buffer.get()) {
                case 0x04:
                    parse_request_SOCKS4();
                    break;
                case 0x05:
                    parse_request_SOCKS5();
                    break;
                default:
                    break;
            }
            //Should we clear buf ?
        } catch (IOException e) {
            shutdown_task();
            e.printStackTrace();
        }

    }

    @Override
    public void parse_request_SOCKS4() throws IOException {
        buffer = ByteBuffer.allocate(7);

        receive_buffer(client);

        byte command = buffer.get();
        short port = buffer.getShort();
        Integer ip = buffer.getInt();

        System.out.println(command);
        System.out.println(port);
        System.out.println(ip);
        //Authentification

        connect_to_destinationServer_SOCKS4(new byte[]{ip.byteValue()}, port);

    }

    public void connect_to_destinationServer_SOCKS4(byte[] ip_v4, short port) throws IOException {
        buffer = ByteBuffer.allocate(8);
        buffer.rewind();
        buffer.put((byte)0x00);
        try {
            destination_Socket = new Socket(InetAddress.getByAddress(ip_v4), port);
         } catch (SecurityException e){
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        } catch (IllegalArgumentException e){
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        } catch (NullPointerException e){
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        } catch (UnknownHostException e){
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        } catch (IOException e) {
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        }

        buffer.put((byte)0x5a);

        send_buffer(client);

    }

    public void connect_to_destinationServer_SOCKS5(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port){
        try {
            switch (ip_type){
                case 0x01:
                    destination_Socket = new Socket(InetAddress.getByAddress(ip_v4), port);
                    break;
                case 0x03:
                    System.out.println(host);
                    destination_Socket = new Socket(host, port);
                    break;
                case 0x04:
                    destination_Socket = new Socket(InetAddress.getByAddress(ip_v6), port);
                    break;
                default:
                    break;
            }
        } catch (SecurityException e){
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        } catch (IllegalArgumentException e){
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        } catch (NullPointerException e){
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        } catch (UnknownHostException e){
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        } catch (IOException e) {
            buffer.put((byte)0x5b);
            //e.printStackTrace();
        }

    }

    private void send_buffer(Socket socket) throws IOException {
        buffer.rewind();
        socket.getOutputStream().write(buffer.array());
        socket.getOutputStream().flush();
    }

    public void streaming() throws IOException {
        buffer = ByteBuffer.allocate(4096);

        receive_buffer(client);
        send_buffer(destination_Socket);

        //Mb not
        buffer.rewind();

        receive_buffer(destination_Socket);
        send_buffer(client);

        //Mb not
        buffer.rewind();

    }

    @Override
    public void parse_request_SOCKS5() throws IOException {
        buffer = ByteBuffer.allocate(256);

        receive_buffer(client);

        byte count_auth_methods = buffer.get();
        byte []auth_methods = new byte[count_auth_methods];
        buffer.get(auth_methods,1,count_auth_methods-1);
        byte picked_method = pick_auth_method(auth_methods);
        buffer = ByteBuffer.allocate(2);
        buffer.put((byte)0x05);
        buffer.put(picked_method);

        send_buffer(client);

        buffer = ByteBuffer.allocate(262);

        receive_buffer(client);

        buffer.get();
        byte command = buffer.get();
        buffer.get();
        byte ip_type = buffer.get();
        short port;
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
                break;
        }

    }

    public int receive_buffer(Socket socket) throws IOException {
        int i = socket.getInputStream().read(buffer.array());
        buffer.rewind();
        return i;
    }

    public byte pick_auth_method(byte[] methods){
        //TODO
        return 0x00;
    }

    @Override
    public void send_response_SOCKS4(){

    }

    @Override
    public void request_to_destinationServer(){


    }

    @Override
    public void shutdown_task(){

        System.out.println("Bye");

    }


}
