import java.util.concurrent.atomic.AtomicInteger;

public class ThreadData {

    public int rqLow;
    public int rqHigh;
    public AtomicInteger rqVersionWhenLinearized;
    public ValueCell[] result;
    public int resultSize = 0;

}