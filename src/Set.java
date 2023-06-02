import java.util.ArrayList;

public interface Set {
    boolean add(int key, int value);
    boolean contains(int key);
    boolean remove(int key);
    ArrayList<Integer> scan(int s, int t);
}
