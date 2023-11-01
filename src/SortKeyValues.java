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
        if (o1.getKey() > o2.getKey()) {
            return 1;
        }
        if (o1.getKey() < o2.getKey()) {
            return -1;
        } else {
            if(o1.getValue().version.get() > o2.getValue().version.get()){
                return 1;
            }
            if(o1.getValue().version.get() < o2.getValue().version.get()){
                return -1;
            }
            else {
                if(o1.getValue().insertionTime > o2.getValue().insertionTime){
                    return 1;
                }
                if(o1.getValue().insertionTime < o2.getValue().insertionTime){
                    return -1;
                }
            }

        }
        return 0;
    }
}