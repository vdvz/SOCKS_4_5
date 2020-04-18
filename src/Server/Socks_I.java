package Server;

import Exceptions.End;
import Exceptions.NoData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface Socks_I {

    void parse() throws End;

    void setCloseFLag(SocketChannel channel);

    void setValidFLag(SocketChannel channel);

    void connect(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws End;

    byte authentication_method(byte[] methods);

    void receive(SocketChannel from) throws End;

    void send(SocketChannel to) throws End;

    void close_sockets();

    boolean identification(SocketChannel client) throws End;

    boolean request_to_BD(String ID, String PW);

    void shutdown() throws End;

}
