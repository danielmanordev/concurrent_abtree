package benchmark.occabtreewithscan.src;

import abstractions.Set;
import abstractions.SetFactory;

public class OCCABTreeWithScanSet implements Set, SetFactory {

    private OCCABTreeWithScan occABTree;
    int a,b;
    public OCCABTreeWithScanSet(int a, int b){
        this.occABTree = new OCCABTreeWithScan(a,b);
        this.a =a;
        this.b =b;
    }


    @Override
    public int add(int key, int value) {
        return this.occABTree.tryInsert(key,value);
    }

    @Override
    public int contains(int key) {
        return this.occABTree.find(key);
    }

    @Override
    public int remove(int key) {
        return this.occABTree.tryDelete(key);
    }

    @Override
    public int getRange(int[] result, int low, int high) {
        return this.occABTree.scan(result,low,high);
    }

    @Override
    public Set newInstance() {
        return new OCCABTreeWithScanSet(a,b);
    }

    @Override
    public String getName() {
        return "OCCABTreeWithScanSet";
    }
}
