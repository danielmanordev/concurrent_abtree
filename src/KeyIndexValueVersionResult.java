public class KeyIndexValueVersionResult extends Result {

    private int keyIndex;
    private int version;

    public KeyIndexValueVersionResult(int value, int keyIndex, int version, ReturnCode returnCode){
        super(value,returnCode);
        this.keyIndex = keyIndex;
        this.version = version;
    }

    public int getKeyIndex(){
        return this.keyIndex;
    }

    public int getVersion(){
        return this.version;
    }
}
