package Server;

import Exceptions.End;
import Exceptions.NoData;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public interface Task {

    void parse_request_SOCKS4() throws IOException, End;

    void connect_to_destinationServer_SOCKS4(byte[] ip_v4, short port) throws IOException, End;

    boolean request_to_BD(String ID, String PW);

    boolean ident_SOCKS5(SocketChannel client) throws End;

    void connect_to_destinationServer_SOCKS5(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws IOException, End;

    boolean ident_SOCKS4(byte[] id);

    void receive_buffer(SocketChannel socket) throws End;

    void send_buffer(SocketChannel socket) throws End;

    void streaming() throws IOException, NoData, End;

    void parse_request_SOCKS5() throws IOException, End;

    byte pick_auth_method_SOCKS5(byte[] methods);

    void shutdown_task() throws IOException, End;

}
