import Locks.Lock;
import Locks.MCSLock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RQProvider {


    private RQThreadData[] rqThreadData;
    private int rqThreadDataSize;
    private int[] init;
    private int maxNodeSize;
    private Lock lock = new MCSLock();

    private LimboListManager limboListManager = new LimboListManager();
    public static AtomicInteger TIMESTAMP = new AtomicInteger(1);
    public RQProvider(int numberOfThreads, int maxNodeSize) {
        this.rqThreadDataSize = (int)Math.pow(numberOfThreads+20,2);
        this.init = new int[rqThreadDataSize];
        this.rqThreadData = new RQThreadData[rqThreadDataSize];
        this.maxNodeSize = maxNodeSize;

    }

    public Node updateDelete(Node leaf, int kvIndex, KvInfo deletedKey) {

        // deletedKey.deletionTime = TIMESTAMP;
        deletedKey.deletionTime = TIMESTAMP.get();
        int threadId = (int) Thread.currentThread().getId();

        announcePhysicalDeletion(threadId ,deletedKey);

        leaf.keys[kvIndex] = 0;
        leaf.values[kvIndex] = null;

        leaf.size = leaf.size-1;
        physicalDeletionSucceeded(threadId, deletedKey);
        return leaf;
    }



    public Node updateInsert(Node leaf, int kvIndex, KvInfo instertedKv) {

        // instertedKv.insertionTime = TIMESTAMP;

        instertedKv.insertionTime = TIMESTAMP.get();


        leaf.keys[kvIndex] = instertedKv.key;
        leaf.values[kvIndex] = instertedKv;

        leaf.size++;

        return leaf;
    }

    // TODO: continue here
    public void traversalStart(int threadId, int low, int high, Node entry) {
        initThread(threadId);
        rqThreadData[threadId].resultSize=0;
        this.rqThreadData[threadId].result = new RQResult[high-low];

        /*lock.lock();
        TIMESTAMP = System.currentTimeMillis();
        long ts = TIMESTAMP;
        lock.unlock();*/

        this.rqThreadData[threadId].rqLinearzationTime = TIMESTAMP.incrementAndGet();
        this.rqThreadData[threadId].low = low;
        this.rqThreadData[threadId].high = high;

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
                 KvInfo kvInfo = leftNode.values[i];
                 if(kvInfo == null){
                     continue;
                 }
                if(kvInfo.key >= low && kvInfo.key <= high && kvInfo.insertionTime < TIMESTAMP.get()){
                    visit(threadId,kvInfo);

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

    private void initThread(int threadId) {
        if(this.init[threadId] == 0){
            this.rqThreadData[threadId] = new RQThreadData();
            this.init[threadId] = 1;
        }
    }



    public void announcePhysicalDeletion(int threadId, KvInfo deletedKey) {
        initThread(threadId);
        this.rqThreadData[threadId].announcements[rqThreadData[threadId].announcementsSize] = deletedKey;
        rqThreadData[threadId].announcementsSize++;
    }



    public void visit(int threadId, KvInfo kvInfo){
        tryAdd(threadId, kvInfo, null, RQSource.DataStructure);
    }

    // TODO: continue here - work with arrays and counters
    public RQResult[] traversalEnd(int threadId){

       for(int i=0;i<this.rqThreadDataSize;i++) {
           if(init[i]==0){
               continue;
           }
           for(int j=0;j<this.rqThreadData[i].announcementsSize;j++)
           {
               KvInfo announcement = rqThreadData[i].announcements[j];
               tryAdd(threadId,announcement, announcement, RQSource.Announcement);
           }

       }

        // Collect pointers to all limbo lists
        // Traverse limbo lists
        int[] threadsIds = this.limboListManager.getThreadsIds();
        int numberOfThreadIds = this.limboListManager.getNumberOfThreadsIds();
        for(int j=0;j<numberOfThreadIds;j++) {
            if(threadsIds[j] == 0){
                continue;
            }
           KvInfo[] limboList=this.limboListManager.getLimboList(threadsIds[j]);
           int limboListSize=this.limboListManager.getLimboListSize();
           for(int i=0;i<limboListSize;i++){
               if(limboList[i] == null){
                   break;
               }
               tryAdd(threadId, limboList[i], null, RQSource.LimboList);
           }
        }

        return this.rqThreadData[threadId].result;
    }


    private void tryAdd(int threadId, KvInfo kvInfo, KvInfo announcedKvInfo, RQSource rqSource) {
        int low = rqThreadData[threadId].low;
        int high = rqThreadData[threadId].high;
        long rqLinearzationTime = rqThreadData[threadId].rqLinearzationTime;

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

            rqThreadData[threadId].result[rqThreadData[threadId].resultSize] = rqResult;
            rqThreadData[threadId].resultSize++;
        }
    }

    private void physicalDeletionSucceeded(int threadId, KvInfo deletedKey) {

        retire(threadId,deletedKey);
        // ensure nodes are placed in the epoch bag BEFORE they are removed from announcements.
        this.rqThreadData[threadId].announcementsSize--;
    }

    private void retire(int threadId, KvInfo kvInfo) {
       this.limboListManager.retire(threadId, kvInfo);
    }

    class RQThreadData {

        int low;
        int high;

        long rqLinearzationTime;

        KvInfo[] announcements = new KvInfo[100];
        int announcementsSize=0;

        RQResult[] result;
        int resultSize=0;

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