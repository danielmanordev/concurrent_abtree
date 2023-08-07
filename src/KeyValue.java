public class KeyValue {

    public int key;
    public KvInfo value;

    public Node node;

    KeyValue(int key, KvInfo value){
        this.key = key;
        this.value = value;

    }

    KeyValue(){

    }


    public int getKey() {
        return this.key;
    }

    public KvInfo getValue(){
        return this.value;
    }

}
