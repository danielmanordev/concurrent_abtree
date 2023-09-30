import java.util.HashSet;

public class ThreadData {

    public ThreadData(int limboListSize) {

        this.limboList = new ValueCell[limboListSize];
    }

    public int rqLow;
    public int rqHigh;

    public int rqVersionWhenLinearized;

    public ValueCell[] rqAnnouncements = new ValueCell[100];
    public int rqAnnouncementsSize=0;

    public ValueCell[] result;
    public int resultSize=0;

    public ValueCell[] limboList;
    public int limboListCurrentIndex;
    public HashSet<ValueCell> vc_hashset = new HashSet<>();

}
