package benchmark.occabtreewithscan.src;


public class KeyValue {

    public int key;
    public ValueCell value;

    public Node node;

    KeyValue(int key, ValueCell value){
        this.key = key;
        this.value = value;

    }

    KeyValue(){

    }


    public int getKey() {
        return this.key;
    }

    public ValueCell getValue(){
        return this.value;
    }

}
