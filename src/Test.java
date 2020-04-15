import DestinationServer.Test_Server;
import Server.*;

import java.io.IOException;

public class Test {

    public static void main(String[] args) {
        //Append user ID or User Id and Password to connect with authorization
        BD.getInstance().appendUser("vizir","vadim");
        //Set MAX CONNECTION for SERVER
        int MAX_CONNECTION = 4;
        //Set requirement port for SERVER
        int PORT = 20;

        Server server = new Server(PORT, MAX_CONNECTION);

        //Undo for start test server, packet for test server have been sending by test client
        /*
        Thread thread = new Thread(new Test_Server());
        thread.start();
        */
        try {
            server.configurate();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
