public class KeyValue {

    public int key;
    public KeyValuePair value;

    public Node node;

    KeyValue(int key, KeyValuePair value){
        this.key = key;
        this.value = value;

    }

    KeyValue(){

    }


    public int getKey() {
        return this.key;
    }

    public KeyValuePair getValue(){
        return this.value;
    }

}
