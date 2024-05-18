public class KeyValue {

    public int key,value;
    public ValueCell valueCell;

    public Node node;

    KeyValue(int key, ValueCell value){
        this.key = key;
        this.valueCell = value;

    }

    KeyValue(int key, int value){
       this.key = key;
       this.value = value;
    }

    KeyValue(){

    }


    public int getKey() {
        return this.key;
    }

    public ValueCell getValueCell(){
        return this.valueCell;
    }

}
