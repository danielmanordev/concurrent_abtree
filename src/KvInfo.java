public class KvInfo implements Comparable<KvInfo> {

    public KvInfo(Node leaf, int index, int key, int value, long insertionTime, long deletionTime) {
        this.key = key;
        this.value = value;
        this.insertionTime = insertionTime;
        this.deletionTime = deletionTime;
        this.leaf = leaf;
        this.index = index;
    }
    public int key;
    public int value;
    public long insertionTime;
    public long deletionTime;

    public Node leaf;
    public int index;

    @Override
    public int compareTo(KvInfo o) {
        return Integer.compare(this.key, o.key);
    }
}
