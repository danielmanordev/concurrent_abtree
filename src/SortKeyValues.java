import java.util.Comparator;

class SortKeyValues implements Comparator<KeyValue> {

    @Override
    public int compare(KeyValue o1, KeyValue o2) {
        if(o1 == null && o2 == null) {
            return 0;
        }
        if(o1 == null ) {
            return 1;
        }
        if(o2 == null ) {
            return -1;
        }
        if(o1.getKey() == 0) {
            return 1;
        }
        if(o2.getKey() == 0) {
            return -1;
        }
        return Integer.compare(o1.getKey(), o2.getKey());
    }
}