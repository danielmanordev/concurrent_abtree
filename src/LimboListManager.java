import java.util.HashMap;
import java.util.Set;


public class LimboListManager {

    private final int LIMBOLIST_SIZE=Integer.MAX_VALUE/2;
    public HashMap<Integer, ThreadData> threadsData = new HashMap<>();


    public void retire(int threadId, KvInfo kvInfo){

        initThreadData(threadId);
        ThreadData currentThreadData = this.threadsData.get(threadId);

        currentThreadData.limboListArray[currentThreadData.index] = kvInfo;
        int nextIndex = currentThreadData.index+1;
        currentThreadData.index = nextIndex%LIMBOLIST_SIZE;
    }

    public Set<Integer> getThreadsIds() {
        return Set.copyOf(threadsData.keySet());
    }

    public KvInfo[] getLimboList(int threadId) {
        return this.threadsData.get(threadId).limboListArray;
    }

    public int getLimboListSize(){
        return this.LIMBOLIST_SIZE;
    }

    private void initThreadData(int threadId){
       this.threadsData.putIfAbsent(threadId, new ThreadData());
    }

    class ThreadData {

        public KvInfo[] limboListArray = new KvInfo[LIMBOLIST_SIZE];
        public int index;

    }

}


