import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class LimboListManager {

    private final int LIMBOLIST_SIZE=1000000;
    private final int NUMBER_OF_THREAD_IDS=10000;

    // public int[] init = new int[NUMBER_OF_THREAD_IDS];
    public ThreadData[] threadsData = new ThreadData[NUMBER_OF_THREAD_IDS];

    public void retire(int threadId, KvInfo kvInfo){

        initThreadData(threadId);
        ThreadData currentThreadData = threadsData[threadId];

        currentThreadData.limboListArray[currentThreadData.index] = kvInfo;
        int nextIndex = currentThreadData.index+1;
        currentThreadData.index = nextIndex%LIMBOLIST_SIZE;
    }

    public Set<Integer> getThreadsIds() {
        return Set.copyOf(threadsData.keySet());
    }

    public KvInfo[] getLimboList(int threadId) {
        return this.threadsData[threadId].limboListArray;
    }

    public int getLimboListSize(){
        return this.LIMBOLIST_SIZE;
    }

    private void initThreadData(int threadId){
        if(threadsData[threadId] == null){
            threadsData[threadId] = new ThreadData();
        }
    }

    class ThreadData {

        public KvInfo[] limboListArray = new KvInfo[LIMBOLIST_SIZE];
        public int index;

    }

}


