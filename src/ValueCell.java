import util.VersionedValue;
import util.VersionsBST;

public class ValueCell implements Comparable<ValueCell> {

    private final VersionsBST versionsBst;
    private VersionedValue latestValue;
    public final int key;

    private void addLatestValueToBST(){
        this.versionsBst.insert(latestValue.getVersion(),latestValue.value);
    }

    public ValueCell(int key){
        this.key = key;
        this.versionsBst = new VersionsBST(key);
    }


    @Override
    public int compareTo(ValueCell o) {
        return Integer.compare(this.key, o.key);
    }

    public boolean casLatestVersion(int expectedValue, int newValue){
        return this.latestValue.casVersion(expectedValue,newValue);
    }

    public boolean setLatestVersion(int latestVersion){
        return this.latestValue.setLatestVersion(latestVersion);
    }

    public int getLatestValue(){
        return this.latestValue.value;
    }

    public VersionedValue getLatestVersionedValue(){
        return this.latestValue;
    }

    public int getLatestValueVersion(){
        return this.latestValue.getVersion();
    }

    public int getValueByVersion(int maxVersion){
        return this.versionsBst.floor(maxVersion);
    }

    public void putNewValue(int value){
       addLatestValueToBST();
       latestValue = new VersionedValue(0,value,this.key);
    }
}
