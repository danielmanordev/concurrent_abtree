import java.util.concurrent.atomic.AtomicInteger;

public class PutData {
    public AtomicInteger version;
    int key;
    int value;

    public PutData(int key, int value) {
        this.key = key;
        this.value = value;
    }
}
