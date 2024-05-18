package util;

public class VersionsBSTNode {
    VersionedValue valueVersion;
    VersionsBSTNode left, right;

    public VersionsBSTNode(VersionedValue valueVersion) {
        this.valueVersion = valueVersion;
        left = right = null;
    }
}
