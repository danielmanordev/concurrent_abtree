import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RQProvider {

    private static final ReentrantReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock(true);
    private static final int MAX_NODES_INSERTED_OR_DELETED_ATOMICALLY = 32;
    private static long TIMESTAMP = System.currentTimeMillis();
    private HashMap<Integer,RQThreadData> rqThreadsData;
    private int maxNodeSize;
    private Lock lock = new MCSLock();
    private Set ds;

    private int[] init;

    public RQProvider(int numberOfThreads, Set ds) {
        this.init = new int[(int)Math.pow(numberOfThreads,2)];

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
        RQThreadData threadData = this.rqThreadsData.get(threadId);

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
            setDeletionTimestamps(ts,linAddr ,leaf);
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
                    visit(threadId,new KvInfo(leftNode.keys[i],leftNode.values[i],leftNode.insertionTimes[i],leftNode.deletionTimes[i]));
                    low++;
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
    }

    private void setDeletionTimestamps(long ts, int index, Node leaf){
        leaf.deletionTimes[index] = ts;
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



    public void visit(int threadId, KvInfo kvInfo){
        //tryAdd(threadId, kvInfo, null, RQSource.DataStructure);
    }

    public ArrayList<RQResult> traversalEnd(int threadId){
        // Collect pointers p1, ..., pk to other processes’ announcements
       /* for(RQThreadData rqtd : this.rqThreadData) {
            for(KvInfo ann : rqtd.announcements) {
                tryAdd(threadId,ann, ann, RQSource.Announcement);
            }
        }
        // Collect pointers to all limbo lists
        // Traverse limbo lists
        for(RQThreadData rqtd : this.rqThreadData) {
            for(KvInfo ann : rqtd.limboList) {
                tryAdd(threadId, ann, null, RQSource.LimboList);
            }
        }*/
        return this.rqThreadsData[threadId].result;

    }


    private void tryAdd(int threadId, KvInfo kvInfo, KvInfo announcedKvInfo, RQSource rqSource) {
        int low = rqThreadsData[threadId].low;
        int high = rqThreadsData[threadId].high;
        long rqLinearzationTime = rqThreadsData[threadId].rqLinearzationTime;

        while (kvInfo.insertionTime == 0){}
        if(kvInfo.insertionTime >= rqLinearzationTime){
           return; // node inserted after RQ
        }
        if(rqSource == RQSource.DataStructure){
            // do nothing: node was not deleted when RQ was linearized
        }
        else if(rqSource == RQSource.LimboList){
            while (kvInfo.deletionTime == 0) {}
            if(kvInfo.deletionTime < rqLinearzationTime){
                return; // node deleted before RQ
            }
        }
        else if(rqSource == RQSource.Announcement) {
            long deletionTime=0;
            while (deletionTime==0 && kvInfo == announcedKvInfo) {
                deletionTime=kvInfo.deletionTime;
            }

            if(deletionTime==0){
                // loop exited because the process removed this announcement
                // if the process deleted node, then it has now set node.dtime
                deletionTime = kvInfo.deletionTime;

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
        if(kvInfo.key >= low && kvInfo.key <= high) {
            RQResult rqResult = new RQResult(kvInfo.key,kvInfo.value);
            if(rqSource == RQSource.LimboList) {
                rqResult.wasDeletedDuringRangeQuery = true;
            }

            rqThreadsData[threadId].result.add(rqResult);
        }
    }


    private void retire(int treadId, KvInfo kvInfo) {
        this.rqThreadsData[treadId].limboList.add(kvInfo);
    }

    class RQThreadData {


        int low;
        int high;

        long rqLinearzationTime;
        KvInfo[] announcements = new KvInfo[MAX_NODES_INSERTED_OR_DELETED_ATOMICALLY];
        ConcurrentLinkedQueue<KvInfo> limboList = new ConcurrentLinkedQueue<>();
        int numberOfAnnouncements=0;
        int limboListSize = 0;
        HashSet<KvInfo> hashSet;
        ArrayList<RQResult> result = new ArrayList();

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