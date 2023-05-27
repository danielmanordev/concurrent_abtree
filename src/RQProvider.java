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

    private void traversalStart(int threadId) {
        READ_WRITE_LOCK.writeLock().lock();
        this.rqThreadData[threadId].rqLinearzationTime = System.currentTimeMillis();
        READ_WRITE_LOCK.writeLock().unlock();
    }

    private void announcePhysicalDeletion(int threadId, Node[] deletedNodes) {
        int i;
        for(i=0;i<deletedNodes.length;++i){
            this.rqThreadData[threadId].announcements[this.rqThreadData[threadId].numberOfAnnouncments+i] = deletedNodes[i];
        }
        this.rqThreadData[threadId].numberOfAnnouncments += i;
    }

    private void tryAdd() {

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
        Node[] announcements = new Node[32];
        long rqLinearzationTime;
        ArrayList limboList = new ArrayList<Node>();
    }
}
