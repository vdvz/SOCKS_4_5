package Server;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ThreadPool{

    /*
    * В пуле лежат потоки
    * Каждый поток это селектор и навешанные на него каналы
    * Так же каждому потоку соответсвует число текущее количество
    *
    *
    * */
    ArrayList<Thread> threads = new ArrayList<>();
    Map<MySelector, Integer> pool = new HashMap<>();
    int MAX_SOCKET_PER_SELECTOR = 100;
    MySelector selector;

    ThreadPool(){
        selector = new MySelector();
        pool.put(selector, 0);
        Thread thread = new Thread(selector);
        thread.start();
        threads.add(thread);
    }

    public synchronized MySelector register(ServerSocketChannel registered, int key, Object attachment){
        MySelector selector = getMinValueThread();
        selector.register(registered, key, attachment);
        pool.replace(selector, pool.get(selector)+1);
        return selector;
    }

    //Регистрируем новый канал в селекторе
    public synchronized SelectionKey register(SocketChannel registered, int key, Object attachment){
        MySelector selector = getMinValueThread();
        pool.replace(selector, pool.get(selector)+1);
        return selector.register(registered, key, attachment);
    }

    public synchronized void setLock(MySelector selector){
        selector.setLock();
    }

    public synchronized void setUnlock(MySelector selector){
        selector.setUnlock();
    }

    //Уменьшает количество обрабатываемых сокетов на 1
    public synchronized void unregister(MySelector selector){
        pool.replace(selector, pool.get(selector)-1);
    }


    //Возвращает самый незагруженный поток
    public MySelector getMinValueThread(){
        System.out.println("POOL SIZE: " + pool.get(selector));
        return selector;
    }

    public void shutdown(){
        Iterator<Thread> iterator = threads.iterator();
        while(iterator.hasNext()){
            Thread thread = iterator.next();
            if(thread.isAlive()){
                thread.interrupt();
            }
            iterator.remove();
        }
    }

}
