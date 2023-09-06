import java.util.Comparator;

public class SortValueCells implements Comparator<ValueCell> {

    public int compare(ValueCell vc1, ValueCell vc2) {
        if(vc1 == null && vc2 == null) {
            return 0;
        }
        if(vc1 == null ) {
            return 1;
        }
        if(vc2 == null ) {
            return -1;
        }
        if(vc1.key == 0) {
            return 1;
        }
        if(vc2.key == 0) {
            return -1;
        }
        if (vc1.key > vc2.key) {
            return 1;
        }
        if (vc1.key < vc2.key) {
            return -1;
        } else {
            return 0;
        }
    }

}
