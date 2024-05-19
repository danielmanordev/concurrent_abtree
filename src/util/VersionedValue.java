package util;

import java.util.concurrent.atomic.AtomicInteger;

public class VersionedValue {
    private int version;
    private AtomicInteger atomicVersion = new AtomicInteger();
    public int key;
    public int value;
    public boolean casVersion(int expectedVersion, int newVersion){
        if (atomicVersion.compareAndSet(expectedVersion,newVersion)){
            version = newVersion;
            return true;
        }
        else return false;
    }

    public boolean setLatestVersion(int version){
        if(this.version != 0){
            return false;
        }
        this.version = version;
        return true;
    }

    public VersionedValue(int version, int value, int key){
        this.version = version;
        this.key = key;
        this.value = value;
    }

    public int getVersion(){
        return this.version;
    }
}
