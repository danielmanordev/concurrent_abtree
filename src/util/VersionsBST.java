package util;

public class VersionsBST {
    VersionsBSTNode root;
    int key;
    // Constructor
    public VersionsBST(int key) {
        root = null;
        this.key = key;
    }

    public void put(int version, int value) {
        var vv = new VersionedValue(version,value,this.key);
        VersionsBSTNode bstNode = new VersionsBSTNode(vv);
        if (root == null) {
            root = bstNode;
            return;
        }

        VersionsBSTNode parent = null, x = root;
        while (x != null) {
            parent = x;
             int existingVersion = x.valueVersion.getVersion();
            if      (version < existingVersion) x = x.left;
            else if (version > existingVersion) x = x.right;
            else {
                x.valueVersion.value = value;
                return;
            }
        }
         if (version < parent.valueVersion.getVersion()) parent.left  = bstNode;
        else         parent.right = bstNode;
    }


    int get(int version) {
        VersionsBSTNode bstNode = root;
        while (bstNode != null) {
            int existingVersion = bstNode.valueVersion.getVersion();
            if      (version < existingVersion) bstNode = bstNode.left;
            else if (version > existingVersion) bstNode = bstNode.right;
            else return bstNode.valueVersion.value;
        }
        return 0;
    }

    public int floor(int version) {
        int floor = 0;
        VersionsBSTNode curr = this.root;

        while (curr != null) {
            var vv = curr.valueVersion;
            if(vv == null){
                break;
            }

            if (vv.getVersion() == version) {
                floor = vv.value;
                return floor;
            }

            if (version > vv.getVersion()) {

                floor = vv.value;
                curr = curr.right;
            } else {

                curr = curr.left;
            }
        }
        return floor;
    }
}



 