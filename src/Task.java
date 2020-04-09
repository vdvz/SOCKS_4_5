import java.io.IOException;

public interface Task {

    void parse_request_SOCKS4() throws IOException;

    void parse_request_SOCKS5() throws IOException;

    void send_response_SOCKS4();

    void request_to_destinationServer();

    void shutdown_task() throws IOException;

}
