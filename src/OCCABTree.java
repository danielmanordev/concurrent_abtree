import java.util.Arrays;
import java.util.Comparator;

class SortKeyValues implements Comparator<KeyValue> {

    @Override
    public int compare(KeyValue o1, KeyValue o2) {
        if(o1 == null || o2 == null) {
            return -1;
        }
        if (o1.getKey() > o2.getKey()) {
            return 1;
        }
        if (o1.getKey() < o2.getKey()) {
            return -1;
        } else {
            return 0;
        }
    }
}


public class OCCABTree implements Set {

    private int MAX_NODE_SIZE = 11;
    private int MIN_NODE_SIZE = 2;
 
    private int NULL = 0;
    private Node entry;

    private int a;
    private int b;


    public OCCABTree(int a, int b) {
        this.a = a;
        this.b = b;
        int anyKey = 26;
        Node entryLeft = createExternalNode(true,0,anyKey);
        entry = createInternalNode(true,1,anyKey);
        entry.nodes[0] = entryLeft;
    }

    private Result searchLeaf(Node leaf, int key) {
        if(!leaf.isLeaf()) {
            return new Result(NULL, ReturnCode.FAILURE);
        }
        while (true) {
            int ver1 = leaf.ver.get();
            if (ver1 % 2 != 0) {
                continue;
            }

            int val = NULL;
            for (int keyIndex = 0; keyIndex < MAX_NODE_SIZE - 1; keyIndex++) {
                if (leaf.keys[keyIndex] == key) {
                    val = leaf.values[keyIndex];
                    break;
                }
            }
            int ver2 = leaf.ver.get();
            if (ver1 != ver2) {
                continue;
            }
            if (val == NULL) {
                return new Result(NULL, ReturnCode.FAILURE);
            } else {
                return new Result(val, ReturnCode.SUCCESS);
            }
        }
    }

    private Result insert(PathInfo pathInfo, int key, int value) {
        Node node = pathInfo.n;
        Node parent = pathInfo.p;

        assert(node.isLeaf());
        assert(!parent.isLeaf());

        node.lock();

        if(node.isMarked()){
            return new Result(ReturnCode.RETRY);
        }

       for (int i = 0; i < Constants.DEGREE; ++i) {
            if (node.keys[i] == key) {
                return new Result(ReturnCode.FAILURE);
            }
        }

        // At this point, we are guaranteed key is not in node
        int currSize = node.size;
        if(currSize < b) {
            for (int i = 0; i < Constants.DEGREE; ++i) {
                if (node.keys[i] == NULL) {
                    int oldVersion = node.ver.get();
                    node.ver.set(oldVersion+1);
                    node.keys[i] = key;
                    node.values[i] = value;
                    ++node.size;
                    node.ver.set(oldVersion+2);
                    node.unlock();
                    return new Result(value,ReturnCode.SUCCESS);
                }
            }
        }
        else {
            parent.lock();
            if(parent.isMarked()) {
                parent.unlock();
                node.unlock();
                return new Result(ReturnCode.RETRY);
            }

            // OVERFLOW
            // We do not have room for this key, we need to make new nodes so it fits
            // first, we create a std::pair of large arrays
            // containing too many keys and pointers to fit in a single node
            int keyValuesSize = Constants.DEGREE + 1;
            KeyValue[] keyValues = new KeyValue[keyValuesSize];

            int k=0;
            for (int i = 0; i < Constants.DEGREE; i++) {
                if(node.keys[i] != NULL){
                    keyValues[k] = new KeyValue(node.keys[i], node.values[i]);
                    ++k;
                }

            }
            keyValues[k] = new KeyValue(key, value);
            ++k;

            Arrays.sort(keyValues, new SortKeyValues());

            // create new node(s):
            // since the new arrays are too big to fit in a single node,
            // we replace l by a new subtree containing three new nodes:
            // a parent, and two leaves;
            // the array contents are then split between the two new leaves

            int leftSize = k / 2;
            Node left = createExternalNode(true,leftSize, keyValues[0].getKey());
            for (int i = 0; i < leftSize; i++) {
                left.keys[i] = keyValues[i].getKey();
                left.values[i] = keyValues[i].getValue();
            }

            int rightSize = (Constants.DEGREE+1) - leftSize;
            Node right = createExternalNode(true,rightSize, keyValues[leftSize].getKey());
            for (int i = 0; i < rightSize; i++) {
                right.keys[i] = keyValues[i+leftSize].getKey();
                right.values[i] = keyValues[i+leftSize].getValue();
            }


            Node replacementNode = createInternalNode(parent == entry, 2,  keyValues[leftSize].getKey());
            replacementNode.keys[0] = keyValues[leftSize].getKey();
            replacementNode.nodes[0] = left;
            replacementNode.nodes[1] = right;
            replacementNode.isTagged = true;

            // If the parent is not marked, parent->ptrs[info.nodeIndex] is guaranteed to contain
            // node since any update to parent would have deleted node (and hence we would have
            // returned at the node->marked check)

            parent.nodes[pathInfo.nIdx] = replacementNode;
            node.mark();
            node.unlock();
            parent.unlock();
            fixTagged(replacementNode);
            return new Result(ReturnCode.SUCCESS);
        }
        return new Result(ReturnCode.RETRY);

    }

    private Node createExternalNode(boolean weight, int size, int searchKey){
        Node node = createInternalNode(weight, size, searchKey);
        node.setAsLeaf();
        return node;
    }

    private Node createInternalNode(boolean weight, int size, int searchKey){
       return new Node(weight,size,searchKey);
    }

    private Result tryInsert(int key, int value) {
        PathInfo pathInfo = new PathInfo();
        while (true) {
            Result searchResult = search(key,null,pathInfo);
            if(searchResult.getReturnCode() == ReturnCode.SUCCESS){
                return searchResult;
            }

            Result insertResult = insert(pathInfo,key,value);
            ReturnCode insertRetCode = insertResult.getReturnCode();
            if (insertRetCode == ReturnCode.SUCCESS || insertRetCode == ReturnCode.FAILURE) {
                return insertResult;
            }

        }
        }


    private void lockAllNodes(PathInfo pathInfo) {
        pathInfo.n.lock();
        pathInfo.p.lock();
        if (pathInfo.gp != null) {
            pathInfo.gp.lock();
        }

    }

    private void unlockAllNodes(PathInfo pathInfo) {
        pathInfo.n.unlock();
        pathInfo.p.unlock();
        pathInfo.gp.unlock();
    }

    // TODO: Continue here
    private ReturnCode fixTagged(Node node) {
        while (true) {

            if(node.getWeight()) {
                return ReturnCode.UNNECCESSARY;
            }

            // assert: viol is internal (because leaves always have weight = 1)
            assert(!node.isLeaf());
            // assert: viol is not entry or root (because both should always have weight = 1)
            assert(node != entry && node != entry.nodes[0]);


            PathInfo pathInfo = new PathInfo();
            Result result = search(node.searchKey, node, pathInfo);

            if (result.getReturnCode() != ReturnCode.SUCCESS) {
                return ReturnCode.UNNECCESSARY;
            }

            if (pathInfo.n != node) {
                return ReturnCode.UNNECCESSARY;
            }

            // we cannot apply this update if p has a weight violation
            // so, we check if this is the case, and, if so, try to fix it
            if (!pathInfo.p.getWeight()) {
                fixTagged(pathInfo.p);
                continue;
            }

            lockAllNodes(pathInfo);

            if (node.isMarked() || pathInfo.p.isMarked()) {
                unlockAllNodes(pathInfo);
                continue;
            }

            if (pathInfo.gp != null) {
                if (pathInfo.gp.isMarked()) {
                    unlockAllNodes(pathInfo);
                    continue;
                }
            }


            Node gParent = pathInfo.gp;
            Node parent = pathInfo.p;
            Node n = pathInfo.n;
            int size = parent.size + n.size - 1;



            if (size <= b) {
                Node newNode = createInternalNode(true,size,0);
                System.arraycopy(parent.nodes, 0, newNode.nodes, 0, pathInfo.nIdx);
                System.arraycopy(n.nodes, 0, newNode.nodes, pathInfo.nIdx, n.size);
                System.arraycopy(parent.nodes, pathInfo.nIdx + 1, newNode.nodes, pathInfo.nIdx + n.size, parent.size - (pathInfo.nIdx + 1));

                System.arraycopy(parent.keys, 0, newNode.keys, 0, pathInfo.nIdx);
                System.arraycopy(n.keys, 0, newNode.keys, pathInfo.nIdx, getKeyCount(n));
                System.arraycopy(parent.keys, pathInfo.nIdx, newNode.keys, pathInfo.nIdx + getKeyCount(n), getKeyCount(parent) - (pathInfo.nIdx));
                newNode.searchKey = newNode.keys[0];

                gParent.nodes[pathInfo.pIdx] = newNode;
                node.mark();
                parent.mark();
                unlockAllNodes(pathInfo);
                return ReturnCode.SUCCESS;
            } else {
                /**
                 * Split
                 */

                int keys[] = new int[Constants.DEGREE * 2];
                Node nodes[] = new Node[Constants.DEGREE * 2];

                System.arraycopy(parent.nodes, 0, nodes, 0, pathInfo.nIdx);
                System.arraycopy(n.nodes, 0, nodes, pathInfo.nIdx, n.size);
                System.arraycopy(parent.nodes, pathInfo.nIdx + 1, nodes, pathInfo.nIdx + n.size, parent.size - (pathInfo.nIdx + 1));
                System.arraycopy(parent.keys, 0, keys, 0, pathInfo.nIdx);
                System.arraycopy(n.keys, 0, keys, pathInfo.nIdx, getKeyCount(n));
                System.arraycopy(parent.keys, pathInfo.nIdx, keys, pathInfo.nIdx + getKeyCount(node), getKeyCount(parent) - pathInfo.nIdx);

                // the new arrays are too big to fit in a single node,
                // so we replace p by a new internal node and two new children.
                //
                // we take the big merged array and split it into two arrays,
                // which are used to create two new children u and v.
                // we then create a new internal node (whose weight will be zero
                // if it is not the root), with u and v as its children.

                // create new node(s)
                int leftSize = size / 2;
                Node left = createInternalNode(true,leftSize, keys[0]);
                System.arraycopy(keys, 0, left.keys, 0, leftSize - 1);
                System.arraycopy(nodes, 0, left.nodes, 0, leftSize);

                int rightSize = size - leftSize;
                Node right = createInternalNode(true,rightSize, keys[leftSize]);
                System.arraycopy(keys, leftSize, right.keys, 0, rightSize - 1);
                System.arraycopy(nodes, leftSize, right.nodes, 0, rightSize);

                // note: keys[Node - 1] should be the same as n->keys[0]
                Node newNode = createInternalNode(gParent == entry, 2, keys[leftSize - 1]);
                newNode.isTagged = true;
                newNode.keys[0] = keys[leftSize-1];
                newNode.nodes[0] = left;
                newNode.nodes[1] = right;
                gParent.nodes[pathInfo.pIdx] = newNode;
                node.mark();
                parent.mark();

                unlockAllNodes(pathInfo);
                fixTagged(newNode);
                return ReturnCode.SUCCESS;
            }
        }

    }


    private Result search(int key, Node targetNode, PathInfo pathInfo) {
        pathInfo.gp = null;
        pathInfo.p = entry;
        pathInfo.n = entry.nodes[0];
        pathInfo.nIdx = 0;


        while (!pathInfo.n.isLeaf() && (targetNode == null || pathInfo.n != targetNode)) {

            if (pathInfo.n == targetNode) {
                break;
            }

            pathInfo.gp = pathInfo.p;
            pathInfo.p = pathInfo.n;
            pathInfo.pIdx = pathInfo.nIdx;
            pathInfo.nIdx = getChildIndex(pathInfo.n, key);
            pathInfo.n = pathInfo.n.nodes[pathInfo.nIdx];
        }

        if (targetNode != null) {
            if (pathInfo.n == targetNode) {
                return new Result(ReturnCode.SUCCESS);
            } else {
                return new Result(ReturnCode.FAILURE);
            }
        } else {
            KeyIndexValueVersionResult keyIndexValueVersionResult = getKeyIndexValueVersion(pathInfo.n,key);
            return new Result(keyIndexValueVersionResult.getValue(),keyIndexValueVersionResult.getReturnCode());
        }

    }

    KeyIndexValueVersionResult getKeyIndexValueVersion(Node node, int key) {
        int keyIndex;
        int value;
        int version;

        do {
            while (((version = node.ver.get()) & 1) != 0) {}
            keyIndex = 0;
            while (keyIndex < Constants.DEGREE && node.keys[keyIndex] != key) {
                ++keyIndex;
            }
            value = keyIndex < Constants.DEGREE ? node.values[keyIndex] : NULL;
        } while (node.ver.get() != version);
        return value == NULL ? new KeyIndexValueVersionResult(NULL,NULL,NULL,ReturnCode.FAILURE) : new KeyIndexValueVersionResult(value,keyIndex,version,ReturnCode.SUCCESS);

    }


    private int getChildIndex(Node node, int key) {
        int numberOfKeys = getKeyCount(node);
        int retval = 0;

        while (retval < numberOfKeys && key >= node.keys[retval]) {
            ++retval;
        }
        return retval;

    }

    private Result find(int key) {

        PathInfo pathInfo = new PathInfo();
        Result searchResult = search(key, null, pathInfo);

        if(searchResult.getReturnCode() != ReturnCode.SUCCESS){
            return new Result(ReturnCode.FAILURE);
        }
        Node leaf = pathInfo.n;
        Result searchLeafResult = searchLeaf(leaf, key);

        return searchLeafResult;
    }

    private Result tryDelete(int key) {
        PathInfo pathInfo = new PathInfo();
        while (true) {
            Result result = search(key, null, pathInfo);

            if(result.getReturnCode() == ReturnCode.FAILURE){
                return new Result(ReturnCode.NO_VALUE);
            }

            Result deleteResult = delete(pathInfo, key);

            if(deleteResult.getReturnCode() == ReturnCode.SUCCESS){
                return deleteResult;
            }

            if (deleteResult.getReturnCode() == ReturnCode.FAILURE) {
                return deleteResult;
            }

        }
    }

    private Result delete(PathInfo pathInfo, int key) {
        Node node = pathInfo.n;
        Node parent = pathInfo.p;
        Node grandParent = pathInfo.gp;

        node.lock();

        if (node.isMarked()) {
            return new Result(ReturnCode.RETRY);
        }
        int newSize = node.size - 1;
        int deletedValue = NULL;
        for (int i = 0; i < Constants.DEGREE; ++i) {
           if(node.keys[i] == key) {
               deletedValue = node.values[i];
               int oldVersion = node.ver.get();
               node.ver.set(oldVersion);
               node.keys[i] = 0;
               node.size = newSize;
               node.ver.set(oldVersion+2);

               if(newSize == a-1) {
                   node.unlock();
                   fixUnderfull(node);
               }
               node.unlock();
               return new Result(deletedValue, ReturnCode.SUCCESS);
           }
        }
        return new Result(ReturnCode.FAILURE);
    }

    private Result fixUnderfull(Node underFullNode) {
       Node parent,gParent,node,sibling;

       while (true) {
           // We do not need a lock for the viol == entry->ptrs[0] check since since we cannot
           // "be turned into" the root. The root is only created by the root absorb
           // operation below, so a node that is not the root will never become the root.
           if(underFullNode.size >= a || underFullNode == entry || underFullNode == entry.nodes[0]) {
               return new Result(ReturnCode.UNNECCESSARY);
           }

           /**
            * search for viol
            */
           PathInfo pathInfo = new PathInfo();
           search(underFullNode.searchKey,underFullNode,pathInfo);
           node = pathInfo.n;
           parent = pathInfo.p;
           gParent = pathInfo.gp;

           // Technically this only matters if the parent has fewer than 2 pointers.
           // Maybe should change the check to that?
           if (parent.size < a && parent != entry && parent != entry.nodes[0]) {
               fixUnderfull(parent);
               continue;
           }

           if (node != underFullNode) {
               // viol was replaced by another update.
               // we hand over responsibility for viol to that update.
               return new Result(ReturnCode.UNNECCESSARY);
           }

           int siblingIndex = (pathInfo.nIdx > 0 ? pathInfo.nIdx - 1 : 1);

           sibling = parent.nodes[siblingIndex];

           int leftIndex;
           int rightIndex;
           Node left;
           Node right;

           if(siblingIndex < pathInfo.nIdx) {
               left = sibling;
               right = node;
               leftIndex = siblingIndex;
               rightIndex = pathInfo.nIdx;


               sibling.lock();
               node.lock();
               if (sibling.isMarked() || node.isMarked()) {
                   sibling.unlock();
                   node.unlock();
                   continue;
               } // RETRY

           }
           else {
               left = node;
               right = sibling;
               leftIndex = pathInfo.nIdx;
               rightIndex = siblingIndex;

               sibling.lock();
               node.lock();
               if (sibling.isMarked() || node.isMarked()) {
                   sibling.unlock();
                   node.unlock();
                   continue;
               } // RETRY
           }

               if(underFullNode.size >= a){
                   return new Result(ReturnCode.UNNECCESSARY);
               }

               parent.lock();
               gParent.lock();
               if(gParent.isMarked() || parent.isMarked()){
                   gParent.unlock();
                   parent.unlock();
                   continue;
               }

               // we can only apply AbsorbSibling or Distribute if there are no
               // weight violations at parent, node, or sibling.
               // So, we first check for any weight violations and fix any that we see.
               if (!parent.getWeight() || !node.getWeight() || !sibling.getWeight()) {
                   node.unlock();
                   sibling.unlock();
                   parent.unlock();
                   gParent.unlock();
                   fixTagged(parent);
                   fixTagged(node);
                   fixTagged(sibling);
                   continue;
               }

           // assert: there are no weight violations at parent, node or sibling
           //assert(parent.getWeight() && node.getWeight() && sibling.getWeight());
           // assert: l and s are either both leaves or both internal nodes
           //         (because there are no weight violations at these nodes)
           assert((node.isLeaf() && sibling.isLeaf()) || (!node.isLeaf() && !sibling.isLeaf()));

           // also note that p->size >= a >= 2

           int lsize = left.size;
           int rsize = right.size;
           int psize = parent.size;
           int size = lsize+rsize;

           if (size < 2 * a) {
               /**
                * AbsorbSibling
                */

               Node newNode;
               // create new node(s))
               int keyCounter = 0, ptrCounter = 0;
               if (left.isLeaf()) {
                   //duplicate code can be cleaned up, but it would make it far less readable...
                   Node newNodeExt = createExternalNode(true, size, node.searchKey);
                   for (int i = 0; i < Constants.DEGREE; i++) {
                       if (left.keys[i] != NULL) {
                           newNodeExt.keys[keyCounter++] = left.keys[i];
                           newNodeExt.values[ptrCounter++] = left.values[i];
                       }
                   }
                   assert (right.isLeaf());
                   for (int i = 0; i < Constants.DEGREE; i++) {
                       if (right.keys[i] != NULL) {
                           newNodeExt.keys[keyCounter++] = right.keys[i];
                           newNodeExt.values[ptrCounter++] = right.values[i];
                       }
                   }
                   newNode = newNodeExt;
               } else {
                   Node newNodeInt = createInternalNode(true, size, node.searchKey);
                   for (int i = 0; i < getKeyCount(left); i++) {
                       newNodeInt.keys[keyCounter++] = left.keys[i];
                   }
                   newNodeInt.keys[keyCounter++] = parent.keys[leftIndex];
                   for (int i = 0; i < lsize; i++) {
                       newNodeInt.nodes[ptrCounter++] = left.nodes[i];
                   }
                   assert (!right.isLeaf());
                   for (int i = 0; i < getKeyCount(right); i++) {
                       newNodeInt.keys[keyCounter++] = right.keys[i];
                   }
                   for (int i = 0; i < rsize; i++) {
                       newNodeInt.nodes[ptrCounter++] = right.nodes[i];
                   }
                   newNode = newNodeInt;
               }

               // now, we atomically replace p and its children with the new nodes.
               // if appropriate, we perform RootAbsorb at the same time.
               if (gParent == entry && psize == 2) {
                   assert(pathInfo.pIdx == 0);
                   gParent.nodes[pathInfo.pIdx] = newNode;
                   node.mark();
                   parent.mark();
                   sibling.mark();

                   node.unlock();
                   sibling.unlock();
                   parent.unlock();
                   gParent.unlock();
                   fixUnderfull(newNode);
                   return new Result(ReturnCode.SUCCESS);
               } else {
                   Node newParent = createInternalNode(true, psize - 1, parent.searchKey);
                   for (int i = 0; i < leftIndex; i++) {
                       newParent.keys[i] = parent.keys[i];
                   }
                   for (int i = 0; i < siblingIndex; i++) {
                       newParent.nodes[i] = parent.nodes[i];
                   }
                   for (int i = leftIndex + 1; i < getKeyCount(parent); i++) {
                       newParent.keys[i - 1] = parent.keys[i];
                   }
                   for (int i = pathInfo.nIdx + 1; i < psize; i++) {
                       newParent.nodes[i - 1] = parent.nodes[i];
                   }

                   int index;
                   if(pathInfo.nIdx > siblingIndex){
                       index = 1;
                   } else {
                       index = 0;
                   }
                   newParent.nodes[pathInfo.nIdx - index] = newNode;

                   gParent.nodes[pathInfo.pIdx] = newParent;

                   node.mark();
                   parent.mark();
                   sibling.mark();

                   node.unlock();
                   sibling.unlock();
                   parent.unlock();
                   gParent.unlock();

                   fixUnderfull(newNode);
                   fixUnderfull(newParent);
                   }

               } else { /**
            * Distribute
            */

               int leftSize = size / 2;
               int rightSize = size - leftSize;

               Node newLeft;
               Node newRight;

               KeyValue[] keyValues = new KeyValue[2*Constants.DEGREE];

               for (int i=0;i<2*Constants.DEGREE;i++){
                   keyValues[i] = new KeyValue();
               }
               // combine the contents of l and s (and one key from p if l and s are internal)

               int keyCounter = 0;
               int valCounter = 0;
               if (left.isLeaf()) {
                   assert(right.isLeaf());
                   for (int i = 0; i < Constants.DEGREE; i++) {
                       if (left.keys[i] != NULL) {
                           keyValues[keyCounter++].key = left.keys[i];
                           keyValues[valCounter++].value = left.values[i];
                       }
                   }
               } else {
                   for (int i = 0; i < getKeyCount(left); i++) {
                       keyValues[keyCounter++].key = left.keys[i];
                   }
                   for (int i = 0; i < lsize; i++) {
                       keyValues[valCounter++].node = left.nodes[i];
                   }
               }

               if (!left.isLeaf()) {
                   keyValues[keyCounter++] = new KeyValue(parent.keys[leftIndex]);
               }

               if (right.isLeaf()) {
                   for (int i = 0; i < Constants.DEGREE; i++) {
                       if (right.keys[i] != NULL) {
                          keyValues[keyCounter++].key = right.keys[i];
                          keyValues[valCounter++].value = right.values[i];
                       }
                   }
               } else {
                   for (int i = 0; i < getKeyCount(right); i++) {
                       keyValues[keyCounter++].key = right.keys[i];
                   }
                   for (int i = 0; i < rsize; i++) {
                       keyValues[valCounter++].node = right.nodes[i];
                   }
               }

               if (left.isLeaf()){
                   Arrays.sort(keyValues, new SortKeyValues());
               }

               keyCounter = 0;
               valCounter = 0;
               int pivot;

               if (left.isLeaf()) {
                   Node newLeftExt = createExternalNode(true, leftSize, 0);
                   for (int i = 0; i < leftSize; i++) {
                       newLeftExt.keys[i] = keyValues[keyCounter++].key;
                       newLeftExt.nodes[i] = keyValues[valCounter++].node;
                   }
                   newLeft = newLeftExt;
                   newLeft.searchKey = newLeftExt.keys[0];
                   pivot = keyValues[keyCounter].key;

               } else {
                   Node newLeftInt = createInternalNode(true, leftSize, 0);
                   for (int i = 0; i < leftSize - 1; i++) {
                       newLeftInt.keys[i] = keyValues[keyCounter++].key;
                   }
                   for (int i = 0; i < leftSize; i++) {
                       newLeftInt.nodes[i] = keyValues[valCounter++].node;
                   }
                   newLeft = newLeftInt;
                   newLeft.searchKey = newLeftInt.keys[0];
                   pivot = keyValues[keyCounter++].key;
               }

               // reserve one key for the parent (to go between newleft and newright))

               int index = left.isLeaf() ? 1 : 0;
               if (right.isLeaf()) {

                   Node newRightExt = createExternalNode( true, rightSize, 0);

                   for (int i = 0; i < rightSize - index; i++) {
                       newRightExt.keys[i] = keyValues[keyCounter++].key;
                   }
                   newRight = newRightExt;
                   newRight.searchKey = newRightExt.keys[0]; // TODO: verify searchKey setting is same as llx/scx based version
                   for (int i = 0; i < rightSize; i++) {
                       newRight.nodes[i] = keyValues[valCounter++].node;
                   }
               } else {
                   Node newRightInt = createInternalNode(true, rightSize, 0);
                   for (int i = 0; i < rightSize - index; i++) {
                       newRightInt.keys[i] = keyValues[keyCounter++].key;
                   }
                   newRight = newRightInt;
                   newRight.searchKey = newRightInt.keys[0];
                   for (int i = 0; i < rightSize; i++) {
                       newRight.nodes[i] = keyValues[valCounter++].node;//(nodeptr)tosort[valCounter++].val;
                   }
               }

               // in this case we replace the parent, despite not having to in the llx/scx version...
               // this is a holdover from kcas. experiments show this case almost never occurs, though, so perf impact is negligible.
               Node newParent = createInternalNode(parent.getWeight(), psize, parent.searchKey);
               System.arraycopy(parent.keys, 0, newParent.keys, 0, getKeyCount(parent));
               System.arraycopy(parent.nodes, 0, newParent.nodes, 0, psize);
               newParent.nodes[leftIndex] = newLeft;
               newParent.nodes[rightIndex] = newRight;
               newParent.keys[leftIndex] = pivot;

               gParent.nodes[pathInfo.pIdx] = newParent;
               node.mark();
               parent.mark();
               sibling.mark();


               return new Result(ReturnCode.SUCCESS);

           }
       }
    }


    private int getKeyCount(Node node) {
        return node.isLeaf() ? node.size : node.size - 1;
    }

    @Override
    public boolean add(int key, int value) {
        Result result = tryInsert(key, value);
        return result.getReturnCode() == ReturnCode.SUCCESS;

    }

    @Override
    public boolean contains(int key) {

        Result result = find(key);
        return result.getReturnCode() == ReturnCode.SUCCESS;
    }

    @Override
    public boolean remove(int data) {
        tryDelete(data);
        return false;
    }

    @Override
    public int[] scan(int s, int t) {
        return new int[0];
    }
}
