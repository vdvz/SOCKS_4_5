import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {

    final byte version = 0x04;
    final byte command = 0x01;
    final int port_number = 80;
    final long ip = 2130706433;//127.0.0.1


    public static void main(String[] args) {
        try {
            byte f = 0x05;
            byte s = 0x01;
            short th = 0x00;
            int fo = 122;
            ByteBuffer b = ByteBuffer.allocate(8);
            //b.put(f).put(s).putShort(th).putInt(fo);
            b.put((byte)0x04).put((byte)0x01).putShort((short)80);
            Socket soc = new Socket("localhost", 80);
            soc.getOutputStream().write(b.array());
            soc.getOutputStream().flush();
            b.rewind();
            System.out.println(soc.getInputStream().read(b.array()));
            System.out.println(b.get());
            System.out.println(b.get());

            b = ByteBuffer.allocate(262);
            b.rewind();
            String str = "Hello";
            System.out.println(str.getBytes().length);
            b.put((byte)0x05).put((byte)0x01).put((byte)0x00).put((byte)0x03).put((byte)str.getBytes().length)
                    .put(str.getBytes()).putShort((short)80);
            soc.getOutputStream().write(b.array());
            soc.getOutputStream().flush();

            System.out.println(soc.getInputStream().read(b.array()));


        } catch (IOException e) {
            e.printStackTrace();
        }




    }


}
