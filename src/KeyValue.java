public class KeyValue {

    public int key;
    public Node node;


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

    public Node getValue(){
        return new Node(this.node.key,this.node.value);
    }
}
