import java.util.concurrent.atomic.AtomicInteger;

public class PutData {
    public final int		orderIndex;
    public AtomicInteger version;
    public PutData(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
