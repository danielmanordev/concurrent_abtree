public class ThreadData {
    private int limboListSize;

    public ThreadData(int limboListSize) {
        this.limboListSize = limboListSize;
    }

    int rqLow;
    int rqHigh;

    long rqLinearzationTime;

    KeyValuePair[] rqAnnouncements = new KeyValuePair[100];
    int rqAnnouncementsSize=0;

    OCCABTree.RQResult[] result;
    int resultSize=0;

    public KeyValuePair[] limboList = new KeyValuePair[limboListSize];
    public int limboListCurrentIndex;
}
