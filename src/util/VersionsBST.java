package util;

public class VersionsBST {
    VersionsBSTNode root;
    int key;
    // Constructor
    public VersionsBST(int key) {
        root = null;
        this.key = key;
    }

    public int insert(int version, int value){
        return insertInternal(this.root, new VersionedValue(version,value,this.key)).valueVersion.getVersion();
    }


    // A utility function to insert
    // a new BinarySearchTreeNode with given key in BST
    VersionsBSTNode insertInternal(VersionsBSTNode BinarySearchTreeNode, VersionedValue valueVersion) {
        // If the tree is empty, return a new BinarySearchTreeNode
        if (BinarySearchTreeNode == null) {
            BinarySearchTreeNode = new VersionsBSTNode(valueVersion);
            return BinarySearchTreeNode;
        }

        // Otherwise, recur down the tree
        if (valueVersion.getVersion() < valueVersion.getVersion())
            BinarySearchTreeNode.left = insertInternal(BinarySearchTreeNode.left, valueVersion);
        else if (valueVersion.getVersion() > BinarySearchTreeNode.valueVersion.getVersion())
            BinarySearchTreeNode.right = insertInternal(BinarySearchTreeNode.right, valueVersion);

        // Return the (unchanged) BinarySearchTreeNode pointer
        return BinarySearchTreeNode;
    }

    // Utility function to search a key in a BST
    VersionsBSTNode search(VersionsBSTNode root, int key) {
        // Base Cases: root is null or key is present at root
        if (root == null || root.valueVersion.getVersion() == key)
            return root;

        // Key is greater than root's key
        if (root.valueVersion.getVersion() < key)
            return search(root.right, key);

        // Key is smaller than root's key
        return search(root.left, key);
    }

    public int floor(int version){
        return floorInternal(this.root,version);
    }

    int floorInternal(VersionsBSTNode root, int version)
    {
        if (root == null)
            return Integer.MAX_VALUE;

        /* If root->data is equal to key */
        if (root.valueVersion.getVersion() == version)
            return root.valueVersion.value;

        /* If root->data is greater than the key */
        if (root.valueVersion.getVersion() > version)
            return floorInternal(root.left, version);

        /* Else, the floor may lie in right subtree
    or may be equal to the root*/
        int floorValue = floorInternal(root.right, version);
        return (floorValue <= version) ? floorValue : root.valueVersion.getVersion();
    }
}
 