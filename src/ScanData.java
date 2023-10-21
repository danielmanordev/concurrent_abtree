import java.util.concurrent.atomic.AtomicInteger;

public class ScanData {
    public ScanData (int low, int high){
        this.low = low;
        this.high = high;
    }
    public final int low;
    public final int high;
    public final AtomicInteger version = new AtomicInteger(0);

}
