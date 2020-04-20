package Server.Connection;

import Exceptions.End;
import Exceptions.NoData;
import Server.Server;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface Task_I {

    void receive(SocketChannel from) throws End;

    void shutdown_task() throws IOException, End;

}
