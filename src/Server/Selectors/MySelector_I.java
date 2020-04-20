package Server.Selectors;

import Server.Protocols.Socks_I;
import Server.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadPoolExecutor;

public interface MySelector_I {
    void setBuffer(ByteBuffer buffer_);

    ByteBuffer getBuffer();

    void setLock();

    void setUnlock();

    Selector getSelector();

    SelectionKey register(SocketChannel channel, int ops, Object attach);

    void register(ServerSocketChannel channel, int ops, Object attach);

    void Readable(SocketChannel in, SocketChannel out, Socks_I socks_task) throws IOException;

    void Connectable();

    void Acceptable(ThreadPoolExecutor threadPoolExecutor, ServerSocketChannel serverSocket) throws IOException;

    void shutdown();
}
