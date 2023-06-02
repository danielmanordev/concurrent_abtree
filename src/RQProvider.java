import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RQProvider {

    private static final ReentrantReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock(true);
    private static long TIMESTAMP = System.currentTimeMillis();
    private RQThreadData[] rqThreadData;

    public RQProvider(int numberOfThreads) {
        this.rqThreadData = new RQThreadData[numberOfThreads];
    }

    public Node updateWrite(Node parent, int nIdx, Node node, Node[] insertedNodes, Node[] deletedNodes) {

        int threadId = (int) Thread.currentThread().getId();
        announcePhysicalDeletion(threadId ,deletedNodes);

        READ_WRITE_LOCK.readLock().lock();
        long ts = TIMESTAMP;
        parent.nodes[nIdx] = node;
        READ_WRITE_LOCK.readLock().unlock();

        setInserationTime(ts, insertedNodes);
        setDeletionTime(ts, deletedNodes);

        physicalDeletionSucceeded(threadId, deletedNodes);
        return node;
    }

    public void traversalStart(int threadId, int low, int high) {
        READ_WRITE_LOCK.writeLock().lock();
        TIMESTAMP = System.currentTimeMillis();
        this.rqThreadData[threadId].rqLinearzationTime = TIMESTAMP;
        READ_WRITE_LOCK.writeLock().unlock();
    }

    public void visit(int threadId, Node node){
        tryAdd(threadId, node, null, RQSource.DataStructure);
    }

    public ArrayList<RQResult> traversalEnd(int threadId){
        // Collect pointers p1, ..., pk to other processes’ announcements
        for(RQThreadData rqtd : this.rqThreadData) {
            for(int i=0;i<rqtd.numberOfAnnouncments;i++) {
                tryAdd(threadId,null, rqtd.announcements[i], RQSource.Announcement);
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



    public void announcePhysicalDeletion(int threadId, Node[] deletedNodes) {
        int i;
        for(i=0;i<deletedNodes.length;++i){
            this.rqThreadData[threadId].announcements[this.rqThreadData[threadId].numberOfAnnouncments+i] = deletedNodes[i];
        }
        this.rqThreadData[threadId].numberOfAnnouncments += i;
    }

    private void tryAdd(int threadId, Node node, Node announcedNode, RQSource rqSource) {
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

    private void physicalDeletionSucceeded(int threadId, Node[] deletedNodes) {
        int i;
        for (i=0;i<deletedNodes.length;++i) {
            retire(threadId,deletedNodes[i]);
        }
        // ensure nodes are placed in the epoch bag BEFORE they are removed from announcements.
        this.rqThreadData[threadId].numberOfAnnouncments -= i;
    }

    private void setInserationTime(long ts, Node[] insertedNodes){


        for(int i=0;i<insertedNodes.length;i++) {
            insertedNodes[i].insertionTime = ts;
        }
    }

    private void setDeletionTime(long ts, Node[] deletedNodes){
        for(int i=0;i<deletedNodes.length;i++) {
            deletedNodes[i].deletionTime = ts;
        }
    }

    private void retire(int treadId, Node node) {
        this.rqThreadData[treadId].limboList.add(node);
    }

    class RQThreadData {

        int numberOfAnnouncments;
        int low;
        int high;

        Node[] announcements = new Node[32];
        long rqLinearzationTime;
        ArrayList<Node> limboList = new ArrayList();
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
