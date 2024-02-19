import abstractions.Lock;
import locks.MCSLock;


import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

public class Node {
    private boolean marked;
    private boolean isLeaf = false;
    private final boolean isEntry = false;
    private boolean weight = false;
    private final int maxNodeSize;


    public Node(boolean weight, int size, int searchKey, int maxNodeSize){
        this.weight = weight;
        this.size = size;
        this.searchKey = searchKey;
        this.keys = new int[maxNodeSize];
        this.values = new ValueCell[maxNodeSize];
        this.nodes = new Node[maxNodeSize];
        this.maxNodeSize = maxNodeSize;

    }

    public Node left;
    public Node right;

    public boolean isTagged = false;
    public int size;
    public int[] keys;
    public AtomicInteger ver = new AtomicInteger(0);
    public ValueCell[] values;



    // public PutData[] putArray = new PutData[OCCABTree.MAX_THREADS+1];
    public Node[] nodes;
    public int searchKey;

    public Lock lock = new MCSLock();

    public boolean isLeaf() {
        return this.isLeaf;
    }

    public void mark(){
        this.marked = true;
    }

    public boolean isMarked(){
        return this.marked;
    }

    public boolean getWeight(){
        return this.weight;
    }

    public void unlock() {
       lock.unlock();
    }

    public void lock() {
       lock.lock();
    }

    public void setAsLeaf(){
        this.isLeaf = true;
    }

    /*public void publishPut(PutData putData){
        int idx = (int) (Thread.currentThread().getId() % OCCABTree.MAX_THREADS);
        this.putArray[idx] = putData;
    }*/



    /*public void helpPutInScan(int myVersion, int low, int high){
        for(int i=0; i < OCCABTree.MAX_THREADS; ++i){
            PutData putData = this.putArray[i];
            if(putData == null){
                continue;
            }

            if(putData.key < low || putData.key > high){
                continue;
            }

            int currPutVersion = putData.version;

            if(currPutVersion == 0){
                putData.version.compareAndSet(currPutVersion,myVersion);
            }

        }
    }*/


}
