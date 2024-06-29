package benchmark.occabtreewithscan.src.abstractions;

public interface Set {
    int add(int key, int value);
    int contains(int key);
    int remove(int key);
    int getRange(int[] result, int s, int t);
}
