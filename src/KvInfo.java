public class KvInfo implements Comparable<KvInfo> {

    public KvInfo(int key, int value, long insertionTime, long deletionTime) {
        this.key = key;
        this.value = value;
        this.insertionTime = insertionTime;
        this.deletionTime = deletionTime;
    }
    public int key;
    public int value;
    public long insertionTime;
    public long deletionTime;

    @Override
    public int compareTo(KvInfo o) {
        return Integer.compare(this.key, o.key);
    }
}
