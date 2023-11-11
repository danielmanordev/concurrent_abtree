package util;

public class ModuloMap<V extends Comparable<V>> {

    private ModuloMapNode<V>[] values;
    int size;
    public ModuloMap(int size){
        this.values = (ModuloMapNode<V>[])new Object[size];
        this.size = size;
    }

    public void put(int key, V value){
        ModuloMapNode<V> newItem = new ModuloMapNode<>(key, value);
        if(this.values[key % this.size] == null){
            this.values[key % this.size] = newItem;
        }
        else {
            ModuloMapNode<V> curr = this.values[key % this.size];
            ModuloMapNode<V> prev = null;
            while (true){
                if(curr == null) {
                    prev.next = newItem;
                    newItem.prev = prev;
                    break;
                }
                if(curr.getKey() != newItem.getKey()){
                    prev = curr;
                    curr = curr.next;
                    continue;
                }
                if (curr.compareTo(newItem) < 0){

                    if(curr.prev == null && curr.next == null){
                        this.values[key % this.size] = newItem;
                        break;
                    }
                    newItem.prev = curr.prev;
                    newItem.next = curr.next;

                    if(curr.prev != null){
                        curr.prev.next = newItem;
                    }
                    if(curr.next != null){
                        curr.next.prev = newItem;
                    }
                    break;
                }
                else {
                    break;
                }
            }
        }

    }


}
