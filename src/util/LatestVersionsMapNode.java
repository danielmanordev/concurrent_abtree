package util;

// TODO: can be generic, ex: "ModuleMapNode"
public class LatestVersionsMapNode implements Comparable<LatestVersionsMapNode> {
    int key;
    LatestVersion latestVersion;
    public LatestVersionsMapNode(int key, LatestVersion latestVersion){
        this.key = key;
        this.latestVersion = latestVersion;
    }
    public LatestVersionsMapNode prev;
    public LatestVersionsMapNode next;

    public LatestVersion getLatestVersion() {
        return this.latestVersion;
    }

    public int getKey(){
        return this.key;
    }

    @Override
    public int compareTo(LatestVersionsMapNode o) {
        return this.getLatestVersion().compareTo(o.getLatestVersion());
    }
}
