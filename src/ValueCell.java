import java.util.concurrent.atomic.AtomicInteger;

public class ValueCell implements Comparable<ValueCell> {

    public ValueCell(int key, int value, long insertionTime) {
        this.key = key;
        this.value = value;
        this.insertionTime = insertionTime;

    }
    public int key;
    public int value;
    public long insertionTime;
    public int version;

    @Override
    public int compareTo(ValueCell o) {
        return Integer.compare(this.key, o.key);
    }
}
