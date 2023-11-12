package util;

// TODO: can be generic, ex: "ModuloMap"
public class LatestVersionsMap {

    private final LatestVersionsMapNode[] values;
    private final int size;
    public LatestVersionsMap(int size){
        this.values = new LatestVersionsMapNode[size];
        this.size = size;
    }

    public void clear(){
        for(int i=0;i<size;i++){
            this.values[i] = null;
        }
    }

    public LatestVersion get(int key){
        LatestVersionsMapNode item = this.values[key % this.size];
        while (true){
            if(item == null){
                return null;
            }
            if(item.getKey() == key){
                return item.getLatestVersion();
            }
            item = item.next;
        }
    }

    public void remove(int key){
        LatestVersionsMapNode item = this.values[key % this.size];
        while (true){
            if(item == null){
                return;
            }
            if(item.getKey() == key){
                if(item.prev == null && item.next == null){
                    this.values[key % this.size] = null;
                    return;
                }
                if(item == this.values[key % this.size]){
                    this.values[key % this.size] = item.next;
                    if(item.next != null){
                        item.next.prev = null;
                    }
                    return;
                }
                item.prev.next = item.next;
                if (item.next != null){
                    item.next.prev = item.prev;
                    return;
                }
            }
            item = item.next;
        }


    }

    public void put(int key, LatestVersion latestVersion){
        LatestVersionsMapNode newItem = new LatestVersionsMapNode(key, latestVersion);
        LatestVersionsMapNode curr = this.values[key % this.size];
        if(curr == null){
            this.values[key % this.size] = newItem;
        }
        else {
            LatestVersionsMapNode prev = null;
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

                    if(newItem.prev == null){
                        this.values[key % this.size] = newItem;
                    }

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
