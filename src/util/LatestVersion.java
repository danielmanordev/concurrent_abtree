package util;

public class LatestVersion implements Comparable<LatestVersion> {
    public int key,version, index,value;
    public long insertionTime;

    public LatestVersion(int key, int value, int version, long insertionTime, int index){
        this.key = key;
        this.version = version;
        this.insertionTime = insertionTime;
        this.index = index;
        this.value = value;
    }


    @Override
    public int compareTo(LatestVersion o) {
        if(this.version > o.version){
            return 1;
        }
        if(this.version < o.version){
            return -1;
        }
        if(this.insertionTime > o.insertionTime){
            return 1;
        }
        if(this.insertionTime < o.insertionTime){
            return -1;
        }
        if(this.value == 0){
            return 1;
        }
        if(o.value == 0){
            return -1;
        }
        return 0;
    }
}
