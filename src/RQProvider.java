import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RQProvider {

    private static final ReentrantReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock(true);
    private static long TIMESTAMP = System.currentTimeMillis();
    private RQThreadData[] rqThreadData;

    public RQProvider(int numberOfThreads) {
        this.rqThreadData = new RQThreadData[200];
        for(int i=0;i<200;i++){
            rqThreadData[i] = new RQThreadData();
        }
    }

    public Node updateDelete(Node leaf, int kvIndex, KvInfo deletedKey) {

        deletedKey.deletionTime = TIMESTAMP;
        int threadId = (int) Thread.currentThread().getId();
        announcePhysicalDeletion(threadId ,deletedKey);

        // READ_WRITE_LOCK.readLock().lock();

        leaf.keys[kvIndex] = 0;
        leaf.values[kvIndex] = 0;
        leaf.size = leaf.size-1;
        // READ_WRITE_LOCK.readLock().unlock();

        physicalDeletionSucceeded(threadId, deletedKey);
        return leaf;
    }

    public Node updateInsert(Node leaf, int kvIndex, KvInfo insertedKey) {

        // READ_WRITE_LOCK.readLock().lock();
        long ts = TIMESTAMP;
        // READ_WRITE_LOCK.readLock().unlock();
        leaf.keys[kvIndex] = insertedKey.key;
        leaf.values[kvIndex] = insertedKey.value;
        leaf.insertionTimes[kvIndex] = ts;
        leaf.size++;


        return leaf;
    }



    public void traversalStart(int threadId, int low, int high) {
        READ_WRITE_LOCK.writeLock().lock();
        TIMESTAMP = System.currentTimeMillis();
        this.rqThreadData[threadId].rqLinearzationTime = TIMESTAMP;
        READ_WRITE_LOCK.writeLock().unlock();
    }

    public void announcePhysicalDeletion(int threadId, KvInfo deletedKey) {

        this.rqThreadData[threadId].announcement = deletedKey;
        // this.rqThreadData[threadId].numberOfAnnouncments++;
    }


    /*public void visit(int threadId, Node node){
        tryAdd(threadId, node, null, RQSource.DataStructure);
    }

    public ArrayList<RQResult> traversalEnd(int threadId){
        // Collect pointers p1, ..., pk to other processes’ announcements
        for(RQThreadData rqtd : this.rqThreadData) {
            for(int i=0;i<rqtd.numberOfAnnouncments;i++) {
                tryAdd(threadId,null, rqtd.announcements.get(i), RQSource.Announcement);
            }
        }
        // Collect pointers to all limbo lists
        // Traverse limbo lists
        for(RQThreadData rqtd : this.rqThreadData) {
            for(int i=0;i<rqtd.limboList.size();i++) {
                tryAdd(threadId, rqtd.limboList.get(i), null, RQSource.LimboList);
            }
        }
        return this.rqThreadData[threadId].result;

    }


    /*private void tryAdd(int threadId, Node node, Node announcedNode, RQSource rqSource) {
        int low = rqThreadData[threadId].low;
        int high = rqThreadData[threadId].high;
        long rqLinearzationTime = rqThreadData[threadId].rqLinearzationTime;

        while (node.insertionTime == 0){}
        if(node.insertionTime >= rqLinearzationTime){
            return; // node inserted after RQ
        }
        if(rqSource == RQSource.DataStructure){
            // do nothing: node was not deleted when RQ was linearized
        }
        else if(rqSource == RQSource.LimboList){
            while (node.deletionTime == 0) {}
            if(node.deletionTime < rqLinearzationTime){
                return; // node deleted before RQ
            }
        }
        else if(rqSource == RQSource.Announcement) {
            long deletionTime=0;
            while (deletionTime==0 && announcedNode == node) {
                deletionTime=node.deletionTime;
            }

            if(deletionTime==0){
                // loop exited because the process removed this announcement
                // if the process deleted node, then it has now set node.dtime
                deletionTime = node.deletionTime;

                if(deletionTime == 0) {
                    // the process did not delete node,
                    // but another process might have
                    return;
                }

            }
            if(deletionTime < rqLinearzationTime){
                return; // node deleted before RQ
            }

            if(node.key >= low && node.key <= high) {
                RQResult rqResult = new RQResult(node.key,node.value);
                rqThreadData[threadId].result.add(rqResult);
            }

        }
    }
  */
    private void physicalDeletionSucceeded(int threadId, KvInfo deletedKey) {

        retire(threadId,deletedKey);
        // ensure nodes are placed in the epoch bag BEFORE they are removed from announcements.
        this.rqThreadData[threadId].announcement = null;
    }

    private void retire(int treadId, KvInfo kvInfo) {
        //this.rqThreadData[treadId].limboList[this.rqThreadData[treadId].limboListSize] = kvInfo;
        //this.rqThreadData[treadId].limboListSize++;
    }

    class RQThreadData {

        int numberOfAnnouncments;
        int low;
        int high;

        KvInfo announcement;
        long rqLinearzationTime;
        int limboListSize = 0;
        //KvInfo[] limboList = new KvInfo[10000000];
        ArrayList<RQResult> result = new ArrayList();

    }

    class RQResult {
        RQResult(int key, int value) {
            this.key = key;
            this.value = value;
        }
        int key;
        int value;
    }
}