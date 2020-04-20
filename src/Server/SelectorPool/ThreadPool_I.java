package Server.SelectorPool;

import Server.Selectors.MySelector;
import Server.Server;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public interface ThreadPool_I {
    //Регистрирует новый канал на append
    MySelector register(ServerSocketChannel registered, int key, Object attachment);

    //создает новый пул
    MySelector create_new_MySelector();

    //Регистрируем новый канал в селекторе
    MySelector register(SocketChannel registered, int key, Object attachment);

    void setMaxSocketCount(int new_value);

    //Уменьшает количество обрабатываемых сокетов на 1
    void unregister(MySelector selector);

    //Возвращает самый незагруженный поток
    MySelector getMinValueThread();

    void shutdown();
}
