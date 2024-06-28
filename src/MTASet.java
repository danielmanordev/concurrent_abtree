import abstractions.Set;
import abstractions.SetFactory;

public class MTASet implements Set, SetFactory {

    private OCCABTree occABTree;

    private int a,b;
    public MTASet(int a, int b){
        this.a = a;
        this.b = b;
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

    @Override
    public Set newInstance() {
        return new MTASet(this.a,this.b);
    }
}
