package Server.BD;

import java.util.HashMap;
import java.util.Map;

public class BD implements BD_I {

    HashMap<String, String> bd;

    int SIZE = 10;

    private static BD instance = new BD();

    private BD(){
        bd = new HashMap<>(SIZE);
    }

    public static BD getInstance() {
        return instance;
    }

    @Override
    public void clear() {
        bd.clear();
    }

    @Override
    public void appendUser(String ID, String PW) {
        bd.put(ID,PW);
    }

    @Override
    public void appendUser(String ID) {
        bd.put(ID, "socks4");
    }

    @Override
    public synchronized boolean identByIdAndPw(String ID, String PW) {
        for (Map.Entry<String, String> entry : bd.entrySet()) {
            if (entry.getKey().equals(ID) && entry.getValue().equals(PW)) return true;
        }
        return false;
    }

    @Override
    public synchronized boolean identById(String ID) {
        for (Map.Entry<String, String> entry : bd.entrySet()) {
            if (entry.getValue().equals(ID) && entry.getKey().equals("socks4")) {
                return true;
            }
        }
        return false;
    }
}
