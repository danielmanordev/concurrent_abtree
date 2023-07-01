public class KeyValue {

    public int key, value;
    public long insertionTime, deletionTime;
    public Node node;

    KeyValue(int key, int value, long insertionTime, long deletionTime){
        this.key = key;
        this.value = value;
        this.insertionTime = insertionTime;
        this.deletionTime = deletionTime;
    }

    KeyValue(){

    }


    public int getKey() {
        return this.key;
    }

    public int getValue(){
        return this.value;
    }

    public long getInsertionTime() { return this.insertionTime; }

    public long getDeletionTime() { return this.deletionTime; }
}
