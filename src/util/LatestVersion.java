package util;

public class LatestVersion implements Comparable<LatestVersion> {
    public int key,version, index;
    public long insertionTime;

    public LatestVersion(int key, int version, long insertionTime, int index){
        this.key = key;
        this.version = version;
        this.insertionTime = insertionTime;
        this.index = index;
    }


    @Override
    public int compareTo(LatestVersion o) {
        if(this.version > o.version){
            return 1;
        }
        else if(this.version < o.version){
            return -1;
        }
        else if(this.insertionTime > o.insertionTime){
            return 1;
        }
        else if(this.insertionTime < o.insertionTime){
            return -1;
        }
        return 0;
    }
}
