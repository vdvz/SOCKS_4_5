public interface ConnectionFactory_I {
    //Очищает сокет и возвращает в пул
    void clear();

    //
    void create_socket_pool();


    //Проверяет есть ли доступные сокеты
    boolean isAvailableSockets();

}
