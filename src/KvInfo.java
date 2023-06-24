public class KvInfo {

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
}
