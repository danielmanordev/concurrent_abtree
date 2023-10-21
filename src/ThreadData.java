import java.util.concurrent.atomic.AtomicInteger;

public class ThreadData {

    public ThreadData(){
        rqVersionWhenLinearized = new AtomicInteger(0);

    }
    public int rqLow;
    public int rqHigh;
    public AtomicInteger rqVersionWhenLinearized;
    public ValueCell[] result;
    public int resultSize = 0;

}