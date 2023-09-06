public class KeyIndexValueVersionResult extends Result {

    private final int version;

    public KeyIndexValueVersionResult(int value, int version, ReturnCode returnCode){
        super(value,returnCode);
        this.version = version;
    }

    public int getVersion(){
        return this.version;
    }
}
