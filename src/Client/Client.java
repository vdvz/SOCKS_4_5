package Client;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client implements Runnable {

    final byte version = 0x04;
    final byte command = 0x01;
    final int port_number = 80;
    Integer ip = 2130706433;//127.0.0.1
    String ip_tg = "[l8�";

    ByteBuffer buffer;
    String ID = "vizir";
    String Password = "vadim";

    private void send_buffer(SocketChannel socket) throws IOException {
        buffer.flip();
        System.out.println("SEND: " + socket.write(buffer));
    }

    public void receive_buffer(SocketChannel socket) throws IOException {
        buffer.clear();
        int i  = socket.read(buffer);
        System.out.println("GET BYTES: " + i);
        buffer.rewind();
    }

    public void make_SOCKS5(SocketChannel server) throws IOException {
        buffer = ByteBuffer.allocate(100);
        buffer.put((byte)0x05).put((byte)0x02).put((byte)0x00).put((byte)0x02);
        send_buffer(server);

        receive_buffer(server);
        buffer.get();
        byte code = buffer.get();
        System.out.println("CODE: " + code);
        if(code == 0x02){
            buffer.clear();
            buffer.put((byte)0x01).put((byte)ID.getBytes().length).put(ID.getBytes()).put((byte)Password.getBytes().length).put(Password.getBytes());
            send_buffer(server);
            receive_buffer(server);
            buffer.get();
            if(buffer.get()!=0x00) return;
        }

        buffer.clear();

        buffer.put((byte)0x05).put((byte)0x01).put((byte)0x00).put((byte)0x01).put(InetAddress.getByName("localhost").getAddress()).putShort((short)81);

        send_buffer(server);

        receive_buffer(server);
        buffer.get();
        System.out.println("Is available: " + buffer.get());

        buffer = ByteBuffer.allocate(4096);

        System.out.println("Send to destination server: " + 3);
        buffer.putInt(3);
        send_buffer(server);

        receive_buffer(server);
        System.out.println("get from destination server: " + buffer.getInt());

        while(true){

        }


    }


    public void make_SOCKS4(SocketChannel server) throws IOException {
        buffer = ByteBuffer.allocate(1000);
        buffer.put((byte)0x04).put((byte)0x01).putShort((short)81).putInt(2130706433).put(ID.getBytes());
        send_buffer(server);

        receive_buffer(server);
        buffer.get();
        System.out.println("Is availabel: " + buffer.get());

        buffer = ByteBuffer.allocate(500);
        buffer.rewind();
        System.out.println("Send to destination server: " + 3);
        buffer.putInt(3);
        send_buffer(server);
        buffer.rewind();
        receive_buffer(server);
        System.out.println("get from destination server: " + buffer.getInt());
    }

    public void run() {
        System.out.println("HERE");
        try {
            byte[] ip_v4 = ByteBuffer.allocate(4).putInt(ip).array();
            ByteBuffer tg = ByteBuffer.allocate(4).put(new Integer(149).byteValue()).put(new Integer(154).byteValue()).put(new Integer(167).byteValue()).put(new Integer(51).byteValue());
            SocketChannel soc = SocketChannel.open(new InetSocketAddress(InetAddress.getByAddress(ip_v4), 20));

            make_SOCKS5(soc);

            soc.shutdownOutput();
            soc.shutdownInput();
            soc.finishConnect();
            //soc.close();

        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e){

        }

    }


}
