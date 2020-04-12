import java.io.IOException;
import java.net.Socket;

public interface Task {

    void parse_request_SOCKS4() throws IOException, End;

    void exceptionsSOCKS4(Exception e) throws IOException, End;

    void connect_to_destinationServer_SOCKS4(byte[] ip_v4, short port) throws IOException, End;

    boolean request_to_BD(String ID, String PW);

    void connect_to_destinationServer_SOCKS5(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws IOException, End;

    boolean ident_SOCKS4(byte[] id);

    void streaming() throws IOException, NoData, End;

    void exceptionsSOCKS5(Exception e, byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws IOException, End;

    void parse_request_SOCKS5() throws IOException, End;

    byte pick_auth_method_SOCKS5(byte[] methods);

    void shutdown_task() throws IOException, End;

}
