import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Test {

    public static void main(String[] args) {
        Server server = new Server(80, 1);

        try {
            server.configurate();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 1; i < 10000; i++){
            System.out.println(i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


}
