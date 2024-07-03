import util.VersionedValue;
import util.VersionsBST;

public class ValueCell implements Comparable<ValueCell> {

    private final VersionsBST versionsBst;
    public VersionedValue latestValue;
    public final int key;

    private void addLatestValueToBST(){
        var vv = latestValue;
        if(vv == null){
            return;
        }
        this.versionsBst.put(vv.getVersion(),vv.value);
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
        var vv = this.latestValue;
        if(vv == null){
            return 0;
        }
        return this.latestValue.value;
    }

    public int getLatestVersion(){
        return this.latestValue.getVersion();
    }

    public int helpAndGetValueByVersion(int version){
        var lv = latestValue;
        if(lv == null){
            return 0;
        }
        if(lv.getVersion() == 0){
            lv.casVersion(0,GlobalVersion.Value.get());
        }
        var lvv = lv.getVersion();
        if(lvv <= version){
            return lv.value;
        }
        return this.versionsBst.floor(version);
    }

    public void putNewValue(int value){
       addLatestValueToBST();
       latestValue = new VersionedValue(0,value,this.key);
    }
}
