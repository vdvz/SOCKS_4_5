package Server.BD;

import Server.Server;

public interface BD_I {

    void clear();

    void appendUser(String ID, String PW);

    void appendUser(String ID);

    boolean identByIdAndPw(String ID, String PW);

    boolean identById(String ID);

}
