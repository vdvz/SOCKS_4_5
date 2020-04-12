import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Test {

    public static void main(String[] args) {
        Server server = new Server(20, 1);

        Thread thread = new Thread(new Test_Server());
        thread.start();

        try {
            server.configurate();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
