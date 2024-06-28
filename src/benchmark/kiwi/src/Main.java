

import kiwi.KiWiMap;

import java.io.IOException;
import java.util.concurrent.ExecutionException;




public class Main {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        KiWiMap kiwi = new KiWiMap();
        kiwi.put(1,1);
        kiwi.put(2,2);
        kiwi.put(4,4);
        kiwi.put(5,5);
        Integer[] res = new Integer[4];
        kiwi.getRange(res,0,10);
    }
}