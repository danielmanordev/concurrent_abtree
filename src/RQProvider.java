import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RQProvider {

    private static final ReentrantReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock(true);
    private static final int MAX_NODES_INSERTED_OR_DELETED_ATOMICALLY = 32;
    private final int initArraySize;
    private static long TIMESTAMP = System.currentTimeMillis();
    private HashMap<Integer,RQThreadData> rqThreadsData;
    private int maxNodeSize;
    private Lock lock = new MCSLock();
    private Set ds;

    private int[] init;

    public RQProvider(int numberOfThreads, Set ds) {
        this.initArraySize = (int)Math.pow(numberOfThreads,2);
        this.init = new int[this.initArraySize];

        // maps a thread to its data
        this.rqThreadsData = new HashMap<>();

        this.ds = ds;

    }

    public void initThread(int threadId) {
        if(init[threadId] == 1){
            return;
        }else {
            init[threadId] =1;
        }
        RQThreadData threadData = this.rqThreadsData.put(threadId, new RQThreadData());

        threadData.hashSet = new HashSet<>();
        threadData.numberOfAnnouncements=0;

        /*for (int i=0;i<MAX_NODES_INSERTED_OR_DELETED_ATOMICALLY+1;++i) {
            threadData.announcements[i] = new KvInfo();
        }*/

    }

    public void announcePhysicalDeletion(int threadId, KvInfo deletedNode) {
        RQThreadData rqThreadData = this.rqThreadsData.get(threadId);

        rqThreadData.announcements[rqThreadData.numberOfAnnouncements+1] = deletedNode;

        rqThreadData.numberOfAnnouncements++;
    }

    public void physicalDeletionSucceeded(int threadId, KvInfo deletedNode) {
        RQThreadData rqThreadData = this.rqThreadsData.get(threadId);

        // TODO: place in epoch bag

        rqThreadData.numberOfAnnouncements--;

    }

    public KvInfo linearizeUpdateAtWrite(int threadId, Node leaf,int linAddr, KvInfo linNewVal, KvInfo inserted, KvInfo deleted) {

        if(deleted != null){
            announcePhysicalDeletion(threadId,deleted);
        }


        /// READ LOCK???
        long ts = TIMESTAMP;
        leaf.keys[linAddr] = linNewVal.key;
        leaf.values[linAddr] = linNewVal.value;
        /// READ UNLOCK???

        if(inserted != null) {
            setInsertionTimestamps(ts,linAddr ,leaf);
        }
        if(deleted != null){
            setDeletionTimestamps(ts,deleted);
            physicalDeletionSucceeded(threadId,deleted);
        }
        return linNewVal;
    }



   /* public Node updateDelete(Node leaf, int kvIndex, KvInfo deletedKey) {

        deletedKey.deletionTime = TIMESTAMP;
        int threadId = (int) Thread.currentThread().getId();
        announcePhysicalDeletion(threadId ,deletedKey);

        leaf.keys[kvIndex] = 0;
        leaf.values[kvIndex] = 0;
        leaf.insertionTimes[kvIndex] = 0;
        leaf.deletionTimes[kvIndex] = 0;
        leaf.size = leaf.size-1;
        physicalDeletionSucceeded(threadId, deletedKey);
        return leaf;
    }

    public Node updateInsert(Node leaf, int kvIndex, KvInfo insertedKey) {


        long ts = TIMESTAMP;

        insertedKey.insertionTime = TIMESTAMP;
        leaf.keys[kvIndex] = insertedKey.key;
        leaf.values[kvIndex] = insertedKey.value;
        leaf.insertionTimes[kvIndex] = ts;
        leaf.size++;


        return leaf;
    }*/


    // TODO: continue here
    public void traversalStart(int threadId, int low, int high, Node entry) {
        RQThreadData rqThreadData = this.rqThreadsData.get(threadId);
        rqThreadData.hashSet.clear();

        lock.lock();
        TIMESTAMP = System.currentTimeMillis();
        long ts = TIMESTAMP;
        lock.unlock();


        rqThreadData.rqLinearzationTime = ts;
        rqThreadData.low = low;
        rqThreadData.high = high;

        traverseLeafs(threadId,low,high,entry);
    }

    private void traverseLeafs(int threadId, int low, int high, Node entry) {
        PathInfo pathInfo = new PathInfo();
        pathInfo.gp = null;
        pathInfo.p = entry;
        pathInfo.n = entry.nodes[0];
        pathInfo.nIdx = 0;

        while (!pathInfo.n.isLeaf()) {

            pathInfo.gp = pathInfo.p;
            pathInfo.p = pathInfo.n;
            pathInfo.pIdx = pathInfo.nIdx;
            pathInfo.nIdx = getChildIndex(pathInfo.n, low);
            pathInfo.n = pathInfo.n.nodes[pathInfo.nIdx];

        }
        Node leftNode = pathInfo.n;
        boolean continueToNextNode=true;

        while(true){
            for(int i=0;i<this.maxNodeSize;i++) {

                if(leftNode.keys[i] >= low && leftNode.keys[i] <= high && leftNode.insertionTimes[i] < TIMESTAMP){
                    visit(threadId,leftNode,i);
                    // System.out.println("Key: "+leftNode.keys[i]+ " Value: "+leftNode.values[i]);

                }
                if(leftNode.keys[i]>high) {
                    continueToNextNode = false;
                }
            }
            if(continueToNextNode && leftNode.right != null) {

                leftNode = leftNode.right;
            }
            else {
                break;
            }
        }

    }

    private void setInsertionTimestamps(long ts, int index, Node leaf){
        leaf.insertionTimes[index] = ts;
        leaf.deletionTimes[index] = 0;
    }

    private void setDeletionTimestamps(long ts, KvInfo kvInfo){
        kvInfo.deletionTime = ts;
    }


    private int getChildIndex(Node node, int key) {
        int numberOfKeys = getKeyCount(node);
        int retval = 0;

        while (retval < numberOfKeys && key >= node.keys[retval]) {
            ++retval;
        }
        return retval;

    }

    private int getKeyCount(Node node) {
        return node.isLeaf() ? node.size : node.size - 1;
    }



    public void visit(int threadId, Node leaf, int index){

        tryAdd(threadId, leaf,  index, null ,RQSource.DataStructure);
    }

    public HashSet<RQResult> traversalEnd(int threadId){
        // Collect pointers p1, ..., pk to other processes’ announcements
        for(int i=0;i<this.initArraySize;i++)
        {
            if(i == threadId){
                continue;
            }
            if(init[i] == 1) {
                RQThreadData rqtd = this.rqThreadsData.get(init[i]);
                for (int j=0;j<rqtd.numberOfAnnouncements;i++) {
                    KvInfo announcement = rqtd.announcements[j];
                    tryAdd(threadId,announcement.leaf,announcement.index,announcement,RQSource.Announcement);
                }
            }
        }

        // Collect pointers to all limbo lists
        // Traverse limbo lists
        for(int i=0;i<this.initArraySize;i++)
        {
            if(i == threadId){
                continue;
            }
            if(init[i] == 1) {
                RQThreadData rqtd = this.rqThreadsData.get(init[i]);
                for (KvInfo item : rqtd.limboList) {

                    tryAdd(threadId,null,0,item,RQSource.LimboList);
                }
            }
        }
        return this.rqThreadsData.get(threadId).hashSet;
    }


    private void tryAdd(int threadId, Node leaf, int index, KvInfo kvInfo, RQSource rqSource) {
        RQThreadData rqThreadData = this.rqThreadsData.get(threadId);
        int low = rqThreadData.low;
        int high = rqThreadData.high;
        long rqLinearzationTime = rqThreadData.rqLinearzationTime;

        long insertionTime = leaf.insertionTimes[index];
        while (insertionTime == 0){
            insertionTime = leaf.insertionTimes[index];
        }
        if(insertionTime >= rqLinearzationTime){
           return; // node inserted after RQ
        }
        if(rqSource == RQSource.DataStructure){
            // do nothing: node was not deleted when RQ was linearized
        }
        else if(rqSource == RQSource.LimboList){
            long deletionTime = kvInfo.deletionTime;
            //while (deletionTime == 0) {
            //    deletionTime = leaf.deletionTimes[index];
            // }
            if(deletionTime < rqLinearzationTime){
                return; // node deleted before RQ
            }
        }
        else if(rqSource == RQSource.Announcement) {
            long deletionTime=0;
            while (deletionTime==0 && leaf.keys[index] == kvInfo.key && leaf.values[index] == kvInfo.value && leaf.insertionTimes[index] == kvInfo.insertionTime) {
                deletionTime=leaf.deletionTimes[index];
            }

            if(deletionTime==0){
                // loop exited because the process removed this announcement
                // if the process deleted node, then it has now set node.dtime
                deletionTime = leaf.deletionTimes[index];

                if(deletionTime == 0) {
                    // the process did not delete node,
                    // but another process might have
                    return;
                }

            }
            if(deletionTime < rqLinearzationTime){
                return; // node deleted before RQ
            }
        }
        if(leaf.keys[index] >= low && leaf.keys[index] <= high) {
            RQResult rqResult = new RQResult(leaf.keys[index],leaf.values[index]);
            if(rqSource == RQSource.LimboList) {
                rqResult.wasDeletedDuringRangeQuery = true;
            }
            rqThreadData.hashSet.add(rqResult);
        }
    }


    private void retire(int threadId, KvInfo kvInfo) {
        this.rqThreadsData.get(threadId).limboList.add(kvInfo);

    }

    class RQThreadData {


        int low;
        int high;

        long rqLinearzationTime;
        KvInfo[] announcements = new KvInfo[MAX_NODES_INSERTED_OR_DELETED_ATOMICALLY];
        ConcurrentLinkedQueue<KvInfo> limboList = new ConcurrentLinkedQueue<>();
        int numberOfAnnouncements=0;
        int limboListSize = 0;
        HashSet<RQResult> hashSet;

    }

    class RQResult {
        RQResult(int key, int value) {
            this.key = key;
            this.value = value;
        }
        int key;
        int value;

        public boolean wasDeletedDuringRangeQuery = false;
    }
}