package benchmark;

import abstractions.Set;

import java.util.concurrent.ConcurrentSkipListMap;

public class JavaConcurrentSkipList implements Set {
    private final ConcurrentSkipListMap<Integer, Integer> concurrentSkipListMap;

    public JavaConcurrentSkipList() {
        concurrentSkipListMap = new ConcurrentSkipListMap<>();
    }

    @Override
    public int add(int key, int value) {
        Integer previousValue = concurrentSkipListMap.put(key,value);
        return previousValue == null ? 0 : previousValue;
    }

    @Override
    public int contains(int key) {
        Integer val = concurrentSkipListMap.get(key);
        return val == null ? 0 : val;
    }

    @Override
    public int remove(int key) {
        Integer val = concurrentSkipListMap.remove(key);
        return val == null ? 0 : val;
    }

    @Override
    public int getRange(int[] result, int s, int t) {
        int resultSize = 0;
        for (int i = s; i <= t; i++) {
            Integer value = concurrentSkipListMap.get(i);
            if (value != null) {
                result[resultSize] = concurrentSkipListMap.get(i);
                resultSize++;
            }
        }
        return resultSize;
    }
}
