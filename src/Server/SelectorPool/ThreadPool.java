package Server.SelectorPool;

import Server.Selectors.MySelector;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ThreadPool implements ThreadPool_I{

    /*
    * В пуле лежат потоки
    * Каждый поток это селектор и навешанные на него каналы
    * Так же каждому потоку соответсвует число текущее количество
    * */
    Map<MySelector, Integer> pool = new HashMap<>();
    int MAX_SOCKET_PER_SELECTOR = 100;

    ThreadPool(){
    }


    public ThreadPool(int max_socket_per_selector_){
        MAX_SOCKET_PER_SELECTOR = max_socket_per_selector_;
    }


    //Регистрирует новый канал на append
    @Override
    public synchronized MySelector register(ServerSocketChannel registered, int key, Object attachment){
        MySelector selector = getMinValueThread();
        selector.register(registered, key, attachment);
        pool.replace(selector, pool.get(selector)+1);
        return selector;
    }


    //создает новый пул
    @Override
    public MySelector create_new_MySelector(){
        System.out.println("new pool");
        MySelector selector = new MySelector();
        pool.put(selector, 0);
        new Thread(selector).start();
        return selector;
    }


    //Регистрируем новый канал в селекторе
    @Override
    public synchronized MySelector register(SocketChannel registered, int key, Object attachment){
        MySelector selector = getMinValueThread();
        pool.replace(selector, pool.get(selector)+1);
        selector.register(registered, key, attachment);
        return selector;
    }

    @Override
    public void setMaxSocketCount(int new_value){
        MAX_SOCKET_PER_SELECTOR = new_value;
    }

    //Уменьшает количество обрабатываемых сокетов на 1
    @Override
    public synchronized void unregister(MySelector selector){pool.replace(selector, pool.get(selector)-1);}

    //Возвращает самый незагруженный поток
    @Override
    public MySelector getMinValueThread(){
        if(pool.isEmpty()) return create_new_MySelector();
        for(Map.Entry<MySelector, Integer> entry: pool.entrySet()){
            if(entry.getValue()<MAX_SOCKET_PER_SELECTOR) return entry.getKey();
        }
        return create_new_MySelector();
    }

    @Override
    public void shutdown() {
        pool.forEach((k, v) -> k.shutdown());

    }

}
