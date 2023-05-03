public class KeyValue {

    public int key, value;
    public Node node;

    KeyValue(int key, int value){
        this.key = key;
        this.value = value;
    }

    KeyValue(int key, Node node){
        this.key = key;
        this.node = node;
    }

    KeyValue(int key){
        this.key = key;
    }

    KeyValue(){

    }


    public int getKey() {
        return this.key;
    }

    public int getValue(){
        return this.value;
    }
}
