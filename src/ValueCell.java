import java.util.concurrent.atomic.AtomicInteger;

public class ValueCell implements Comparable<ValueCell> {

    public ValueCell(int key, int value) {
        this.key = key;
        this.value = value;

    }
    public int key;
    public int value;
    public long insertionTime;
    public long deletionTime;
    public AtomicInteger version = new AtomicInteger(0);

    @Override
    public int compareTo(ValueCell o) {
        return Integer.compare(this.key, o.key);
    }
}
