package benchmark.occabtreewithscan.src;

import abstractions.Lock;
import locks.MCSLock;

import java.util.concurrent.atomic.AtomicInteger;

public class Node {

    private boolean marked;
    private boolean isLeaf = false;
    public Node left;
    public Node right;

    public boolean isTagged = false;
    private final boolean isEntry = false;

    private boolean weight = false;

    public Node(boolean weight, int size, int searchKey, int maxNodeSize){
        this.weight = weight;
        this.size = size;
        this.searchKey = searchKey;
        this.keys = new int[maxNodeSize];
        this.values = new ValueCell[maxNodeSize];
        this.nodes = new Node[maxNodeSize];

    }

    public int size;
    public int[] keys;
    public AtomicInteger ver = new AtomicInteger(0);
    public ValueCell[] values;
    public Node[] nodes;
    public int searchKey;

    public Lock lock = new MCSLock();
    public Lock leafLock = new MCSLock();


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
