public class ThreadData {

    public ThreadData(int limboListSize) {

        this.limboList = new KeyValuePair[limboListSize];
    }

    int rqLow;
    int rqHigh;

    long rqLinearzationTime;

    KeyValuePair[] rqAnnouncements = new KeyValuePair[100];
    int rqAnnouncementsSize=0;

    OCCABTree.RQResult[] result;
    int resultSize=0;

    public KeyValuePair[] limboList;
    public int limboListCurrentIndex;
}
