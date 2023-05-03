import java.util.concurrent.atomic.AtomicInteger;

public class Node {

    private boolean marked;
    private boolean isLeaf = false;
    public boolean isTagged = false;
    private boolean isEntry = false;

    private boolean weight = false;

    public Node(boolean weight, int size, int searchKey){
        this.weight = weight;
        this.size = size;
        this.searchKey = searchKey;
    }

    public int size;
    public int[] keys = new int[Constants.DEGREE];
    public AtomicInteger ver = new AtomicInteger(0);
    public int[] values = new int[Constants.DEGREE];
    public Node[] nodes = new Node[Constants.DEGREE];
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

}
