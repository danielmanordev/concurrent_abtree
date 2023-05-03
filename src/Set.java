public interface Set {
    boolean add(int key, int value);
    boolean contains(int key);
    boolean remove(int key);
    int[] scan(int s, int t);
}
