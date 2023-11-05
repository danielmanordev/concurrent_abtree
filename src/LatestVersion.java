public class LatestVersion {
    int key,version, index;
    long insertionTime;

    public LatestVersion(int key, int version, long insertionTime, int index){
        this.key = key;
        this.version = version;
        this.insertionTime = insertionTime;
        this.index = index;
    }
}
