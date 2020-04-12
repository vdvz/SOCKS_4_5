import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {

    final byte version = 0x04;
    final byte command = 0x01;
    final int port_number = 80;
    Integer ip = 2130706433;//127.0.0.1
    ByteBuffer buffer;


    private void send_buffer(Socket socket) throws IOException {
        buffer.rewind();
        System.out.println("send bytes: " + buffer.array().length);
        socket.getOutputStream().write(buffer.array());
        socket.getOutputStream().flush();
        buffer.rewind();
    }

    public void receive_buffer(Socket socket) throws IOException {
        buffer.rewind();
        System.out.println("size capacity " + buffer.capacity());
        int i = socket.getInputStream().read(buffer.array());
        buffer.rewind();
        System.out.println("receive bytes: " + i);
    }

    public void make_SOCKS5(Socket server) throws IOException {
        buffer = ByteBuffer.allocate(3);
        buffer.put((byte)0x05).put((byte)0x01).put((byte)0x00);
        send_buffer(server);

        receive_buffer(server);
        buffer.get();
        System.out.println("Method:" + buffer.get());

        buffer = ByteBuffer.allocate(10);
        buffer.put((byte)0x05).put((byte)0x01).put((byte)0x00).put((byte)0x01).put(InetAddress.getByName("localhost").getAddress()).putShort((short)81);

        send_buffer(server);

        receive_buffer(server);
        buffer.get();
        System.out.println("Is available: " + buffer.get());

        buffer = ByteBuffer.allocate(4096);
        buffer.rewind();
        System.out.println("Send to destination server: " + 3);
        buffer.putInt(3);
        send_buffer(server);
        buffer.rewind();
        receive_buffer(server);
        System.out.println("get from destination server: " + buffer.getInt());


    }


    public void make_SOCKS4(Socket server) throws IOException {
        buffer = ByteBuffer.allocate(8);
        buffer.put((byte)0x04).put((byte)0x01).putShort((short)81).putInt(2130706433);
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
        try {
            byte[] ip_v4 = ByteBuffer.allocate(4).putInt(ip).array();
            Socket soc = new Socket(InetAddress.getByAddress(ip_v4), 20);

            make_SOCKS5(soc);
            //soc.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
