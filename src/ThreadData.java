public class ThreadData {

    public ThreadData(int limboListSize) {

        this.limboList = new ValueCell[limboListSize];
    }

    int rqLow;
    int rqHigh;

    long rqLinearzationTime;

    ValueCell[] rqAnnouncements = new ValueCell[100];
    int rqAnnouncementsSize=0;

    KeyValue[] result;
    int resultSize=0;

    public ValueCell[] limboList;
    public int limboListCurrentIndex;
}
