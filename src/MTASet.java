import abstractions.Set;

public class MTASet implements Set {

    private OCCABTree occABTree;

    public MTASet(){
        this.occABTree = new OCCABTree(4,16,12);
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
}
