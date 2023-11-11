package util;

public class ModuloMapNode<V extends Comparable<V>> implements Comparable<ModuloMapNode<V>>{
    int key;
    V value;
    public ModuloMapNode(int key, V value){
        this.key = key;
        this.value = value;
    }
    public ModuloMapNode<V> prev;
    public ModuloMapNode<V> next;

    public V getValue() {
        return this.value;
    }

    public int getKey(){
        return this.key;
    }

    @Override
    public int compareTo(ModuloMapNode<V> o) {

        return this.value.compareTo(o.getValue());
    }
}
