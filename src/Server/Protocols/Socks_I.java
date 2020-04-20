package Server.Protocols;

import Exceptions.End;
import Exceptions.NoData;
import Server.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface Socks_I {

    Thread getThread();

    void parse() throws End;

    void setCloseFLag();

    void setValidFLag();

    void connect(byte ip_type, byte[] ip_v4, byte[] ip_v6, String host, short port) throws End;

    byte authentication_method(byte[] methods);

    void receive(SocketChannel from) throws End;

    void send(SocketChannel to) throws End;

    void close_sockets();

    boolean identification(SocketChannel client) throws End;

    boolean request_to_BD(String ID, String PW);

    void shutdown() throws End;

}
