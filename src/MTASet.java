import abstractions.Set;

public class MTASet implements Set {

    private OCCABTree occABTree;

    public MTASet(int a, int b){
        this.occABTree = new OCCABTree(a,b);
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
        return this.occABTree.tryInsert(key,0);
    }

    @Override
    public int getRange(int[] result, int low, int high) {
        int numOfScannedKeys = this.occABTree.scan(result,low,high);
        return numOfScannedKeys;
    }
}
