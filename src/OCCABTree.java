
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;


public class OCCABTree {

    /* Constants */
    private final int NULL = 0;
    private final int LIMBOLIST_SIZE = 1000;

    private Node entry;

    private final int minNodeSize;
    private final int maxNodeSize;

    private final ThreadData[] threadsData;
    private final int threadsDataSize;


    public  AtomicInteger TIMESTAMP = new AtomicInteger(1);

    public OCCABTree(int a, int b, int numberOfThreads) {
        this.minNodeSize = a;
        this.maxNodeSize = b;
        int anyKey = 26;
        Node entryLeft = createExternalNode(true,0,anyKey);
        entry = createInternalNode(true,1,anyKey);
        entry.nodes[0] = entryLeft;

        this.threadsDataSize = (int)Math.pow(numberOfThreads+20,2);
        this.threadsData = new ThreadData[threadsDataSize];

    }

    private Result insert(PathInfo pathInfo, int key, int value) {
        Node node = pathInfo.n;
        Node parent = pathInfo.p;

        assert(node.isLeaf());
        assert(!parent.isLeaf());

        node.lock();

        if(node.isMarked()){
            node.unlock();
            return new Result(ReturnCode.RETRY);
        }

       for (int i = 0; i < this.maxNodeSize; ++i) {
            if (node.keys[i] == key) {
                node.unlock();
                return new Result(ReturnCode.FAILURE);
            }
        }

        // At this point, we are guaranteed key is not in node
        int currSize = node.size;
        if(currSize < this.maxNodeSize) {
            for (int i = 0; i < this.maxNodeSize; ++i) {
                if (node.keys[i] == NULL) {
                    int oldVersion = node.ver.get();
                    node.ver.set(oldVersion+1);
                    updateInsert(node,i,new ValueCell(key,value,0,0));
                    // node.keys[i] = key;
                    // node.values[i] = value;
                    // node.insertionTimes[i] = TIMESTAMP;
                    // ++node.size;
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
            int keyValuesSize = this.maxNodeSize + 1;
            KeyValue[] keyValues = new KeyValue[keyValuesSize];

            int k=0;
            for (int i = 0; i < this.maxNodeSize; i++) {
                if(node.keys[i] != NULL){
                    keyValues[k] = new KeyValue(node.keys[i], node.values[i]);
                    ++k;
                }

            }
            keyValues[k] = new KeyValue(key, new ValueCell(key,value,TIMESTAMP.get(),0));
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

            int rightSize = (this.maxNodeSize+1) - leftSize;
            Node right = createExternalNode(true,rightSize, keyValues[leftSize].getKey());
            for (int i = 0; i < rightSize; i++) {
                right.keys[i] = keyValues[i+leftSize].getKey();
                right.values[i] = keyValues[i+leftSize].getValue();

            }


            left.left = node.left;
            left.right = right;
            right.left = left;
            right.right = node.right;

            if(node.left != null){
                node.left.right = left;
            }

            if(node.right != null){
                node.right.left = right;
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
       return new Node(weight,size,searchKey,this.maxNodeSize);
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

            Node n = pathInfo.n;
            Node p = pathInfo.p;
            Node gp = pathInfo.gp;


            if (n != node) {
                return ReturnCode.UNNECCESSARY;
            }

            // we cannot apply this update if p has a weight violation
            // so, we check if this is the case, and, if so, try to fix it
            if (!p.getWeight()) {
                fixTagged(p);
                continue;
            }


            n.lock();
            p.lock();
            if (gp != null) {
               gp.lock();
            }


            if(n.isMarked()){
                unlockAllNodes(pathInfo);
                continue;
            }

            if(p.isMarked()){
                unlockAllNodes(pathInfo);
                continue;
            }

            if (pathInfo.gp != null) {
                if (pathInfo.gp.isMarked()) {
                    unlockAllNodes(pathInfo);
                    continue;
                }
            }

            int size = p.size + n.size - 1;



            if (size <= this.maxNodeSize) {
                Node newNode = createInternalNode(true,size,0);
                System.arraycopy(p.nodes, 0, newNode.nodes, 0, pathInfo.nIdx);
                System.arraycopy(n.nodes, 0, newNode.nodes, pathInfo.nIdx, n.size);
                System.arraycopy(p.nodes, pathInfo.nIdx + 1, newNode.nodes, pathInfo.nIdx + n.size, p.size - (pathInfo.nIdx + 1));

                System.arraycopy(p.keys, 0, newNode.keys, 0, pathInfo.nIdx);
                System.arraycopy(n.keys, 0, newNode.keys, pathInfo.nIdx, getKeyCount(n));
                System.arraycopy(p.keys, pathInfo.nIdx, newNode.keys, pathInfo.nIdx + getKeyCount(n), getKeyCount(p) - (pathInfo.nIdx));
                newNode.searchKey = newNode.keys[0];

                gp.nodes[pathInfo.pIdx] = newNode;
                node.mark();
                p.mark();
                unlockAllNodes(pathInfo);
                return ReturnCode.SUCCESS;
            } else {
                /**
                 * Split
                 */

                int keys[] = new int[this.maxNodeSize * 2];
                Node nodes[] = new Node[this.maxNodeSize * 2];

                System.arraycopy(p.nodes, 0, nodes, 0, pathInfo.nIdx);
                System.arraycopy(n.nodes, 0, nodes, pathInfo.nIdx, n.size);
                System.arraycopy(p.nodes, pathInfo.nIdx + 1, nodes, pathInfo.nIdx + n.size, p.size - (pathInfo.nIdx + 1));
                System.arraycopy(p.keys, 0, keys, 0, pathInfo.nIdx);
                System.arraycopy(n.keys, 0, keys, pathInfo.nIdx, getKeyCount(n));
                System.arraycopy(p.keys, pathInfo.nIdx, keys, pathInfo.nIdx + getKeyCount(node), getKeyCount(p) - pathInfo.nIdx);

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
                Node newNode = createInternalNode(gp == entry, 2, keys[leftSize - 1]);
                newNode.isTagged = true;
                newNode.keys[0] = keys[leftSize-1];
                newNode.nodes[0] = left;
                newNode.nodes[1] = right;
                gp.nodes[pathInfo.pIdx] = newNode;
                node.mark();
                p.mark();

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


        while (!pathInfo.n.isLeaf() && (targetNode == null ? pathInfo.n != targetNode : true)) {

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
        ValueCell value;
        int version;

        do {
            while (((version = node.ver.get()) & 1) != 0) {}
            keyIndex = 0;
            while (keyIndex < this.maxNodeSize && node.keys[keyIndex] != key) {
                ++keyIndex;
            }
            value = keyIndex < this.maxNodeSize ? node.values[keyIndex] : null;
        } while (node.ver.get() != version);
        return value == null ? new KeyIndexValueVersionResult(NULL,NULL,ReturnCode.FAILURE) : new KeyIndexValueVersionResult(value.value,version,ReturnCode.SUCCESS);

    }


    private int getChildIndex(Node node, int key) {
        int numberOfKeys = getKeyCount(node);
        int retval = 0;

        while (retval < numberOfKeys && key >= node.keys[retval]) {
            ++retval;
        }
        return retval;

    }



    private Result delete(PathInfo pathInfo, int key) {
        Node node = pathInfo.n;

        node.lock();

        if (node.isMarked()) {
            node.unlock();
            return new Result(ReturnCode.RETRY);
        }
        int newSize = node.size - 1;
        int deletedValue = NULL;
        for (int i = 0; i < this.maxNodeSize; ++i) {
           if(node.keys[i] == key) {
               deletedValue = node.values[i].value;
               int oldVersion = node.ver.get();
               node.ver.set(oldVersion+1);
               updateDelete(node,i, node.values[i]);
               //node.keys[i] = 0;
               //node.values[i] = 0;
               //node.size = newSize;
               node.ver.set(oldVersion+2);

               if(newSize == this.minNodeSize-1) {
                   node.unlock();
                   fixUnderfull(node);
                   return new Result(deletedValue, ReturnCode.SUCCESS);
               }
               node.unlock();
               return new Result(deletedValue, ReturnCode.SUCCESS);
           }
        }
        node.unlock();
        return new Result(ReturnCode.FAILURE);
    }

    private Result fixUnderfull(Node underFullNode) {
       Node parent,gParent,node,sibling;

       while (true) {
           // We do not need a lock for the viol == entry->ptrs[0] check since since we cannot
           // "be turned into" the root. The root is only created by the root absorb
           // operation below, so a node that is not the root will never become the root.
           if(underFullNode.size >= this.minNodeSize || underFullNode == entry || underFullNode == entry.nodes[0]) {
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
           if (parent.size < this.minNodeSize && parent != entry && parent != entry.nodes[0]) {
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



               node.lock();
               sibling.lock();
               if (sibling.isMarked() || node.isMarked()) {
                   node.unlock();
                   sibling.unlock();
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
                   node.unlock();
                   sibling.unlock();
                   continue;
               } // RETRY
           }

               if(underFullNode.size >= this.minNodeSize){
                   node.unlock();
                   sibling.unlock();
                   return new Result(ReturnCode.UNNECCESSARY);
               }

               parent.lock();
               gParent.lock();
               if(gParent.isMarked() || parent.isMarked()){
                   node.unlock();
                   sibling.unlock();
                   parent.unlock();
                   gParent.unlock();
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

           if (size < 2 * this.minNodeSize) {
               /**
                * AbsorbSibling
                */

               Node newNode;
               // create new node(s))
               int keyCounter = 0, ptrCounter = 0;
               if (left.isLeaf()) {
                   //duplicate code can be cleaned up, but it would make it far less readable...
                   Node newNodeExt = createExternalNode(true, size, node.searchKey);
                   for (int i = 0; i < this.maxNodeSize; i++) {
                       if (left.keys[i] != NULL) {
                           newNodeExt.keys[keyCounter++] = left.keys[i];
                           newNodeExt.values[ptrCounter++] = left.values[i];

                       }
                   }
                   assert (right.isLeaf());
                   for (int i = 0; i < this.maxNodeSize; i++) {
                       if (right.keys[i] != NULL) {
                           newNodeExt.keys[keyCounter++] = right.keys[i];
                           newNodeExt.values[ptrCounter++] = right.values[i];

                       }
                   }
                   newNodeExt.left = left.left;
                   if(left.left != null) {
                       left.left.right = newNodeExt;
                   }

                   newNodeExt.right = right.right;
                   if(right.right != null) {
                       right.right.left = newNodeExt;
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


               KeyValue[] keyValues = new KeyValue[2*this.maxNodeSize];

               for (int i=0;i<2*this.maxNodeSize;i++){
                   keyValues[i] = new KeyValue();
               }
               // combine the contents of l and s (and one key from p if l and s are internal)

               int keyCounter = 0;
               int valCounter = 0;
               if (left.isLeaf()) {
                   assert(right.isLeaf());
                   for (int i = 0; i < this.maxNodeSize; i++) {
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
                   keyValues[keyCounter++].key = parent.keys[leftIndex];
               }

               if (right.isLeaf()) {
                   for (int i = 0; i < this.maxNodeSize; i++) {
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

                   newLeftExt.right = left.right;
                   if(left.right!=null) {
                       left.right.left = newLeftExt;
                   }

                   newLeftExt.left = left.left;
                   if(left.left!=null) {
                       left.left.right = newLeftExt;
                   }


                   for (int i = 0; i < leftSize; i++) {
                       newLeftExt.keys[i] = keyValues[keyCounter++].key;
                       newLeftExt.values[i] = keyValues[valCounter++].value;

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

               int index = left.isLeaf() ? 0 : 1;
               if (right.isLeaf()) {

                   Node newRightExt = createExternalNode( true, rightSize, 0);

                   newRightExt.right = right.right;
                   if(right.right!=null) {
                       right.right.left = newRightExt;
                   }

                   newRightExt.left = right.left;
                   if(right.left!=null) {
                       right.left.right = newRightExt;
                   }

                   for (int i = 0; i < rightSize - index; i++) {
                       newRightExt.keys[i] = keyValues[keyCounter++].key;
                   }
                   newRight = newRightExt;
                   newRight.searchKey = newRightExt.keys[0]; // TODO: verify searchKey setting is same as llx/scx based version
                   for (int i = 0; i < rightSize; i++) {
                       newRight.values[i] = keyValues[valCounter++].value;
                   }
               } else {
                   Node newRightInt = createInternalNode(true, rightSize, 0);
                   for (int i = 0; i < rightSize - index; i++) {
                       newRightInt.keys[i] = keyValues[keyCounter++].key;
                   }
                   newRight = newRightInt;
                   newRight.searchKey = newRightInt.keys[0];
                   for (int i = 0; i < rightSize; i++) {
                       newRight.nodes[i] = keyValues[valCounter++].node;
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

               node.unlock();
               sibling.unlock();
               parent.unlock();
               gParent.unlock();


               return new Result(ReturnCode.SUCCESS);

           }
       }
    }

    private int getKeyCount(Node node) {
        return node.isLeaf() ? node.size : node.size - 1;
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

            ValueCell value =null;
            for (int keyIndex = 0; keyIndex < this.maxNodeSize - 1; keyIndex++) {
                if (leaf.keys[keyIndex] == key) {
                    value = leaf.values[keyIndex];
                    break;
                }
            }
            int ver2 = leaf.ver.get();
            if (ver1 != ver2) {
                continue;
            }
            if (value == null) {
                return new Result(NULL, ReturnCode.FAILURE);
            } else {
                return new Result(value.value, ReturnCode.SUCCESS);
            }
        }
    }


       /* Range query */

        public Node updateDelete(Node leaf, int kvIndex, ValueCell deletedKey) {

            // deletedKey.deletionTime = TIMESTAMP;
            deletedKey.deletionTime = TIMESTAMP.get();
            int threadId = (int) Thread.currentThread().getId();
            initThread(threadId);
            announcePhysicalDeletion(threadId ,deletedKey);

            leaf.keys[kvIndex] = 0;
            leaf.values[kvIndex] = null;

            leaf.size = leaf.size-1;
            physicalDeletionSucceeded(threadId, deletedKey);
            return leaf;
        }



        public Node updateInsert(Node leaf, int kvIndex, ValueCell instertedKv) {

            // instertedKv.insertionTime = TIMESTAMP;

            instertedKv.insertionTime = TIMESTAMP.get();


            leaf.keys[kvIndex] = instertedKv.key;
            leaf.values[kvIndex] = instertedKv;

            leaf.size++;

            return leaf;
        }

        // TODO: continue here
        public void traversalStart(int threadId, int low, int high, Node entry) {
            initThread(threadId);
            this.threadsData[threadId].resultSize=0;
            this.threadsData[threadId].result = new ValueCell[(high-low)+1];
            this.threadsData[threadId].vc_hashset.clear();
            this.threadsData[threadId].rqLinearzationTime = TIMESTAMP.incrementAndGet();
            this.threadsData[threadId].rqLow = low;
            this.threadsData[threadId].rqHigh = high;

            traverseLeafs(threadId,low,high,entry);

        }

        private void traverseLeafs(int threadId, int low, int high, Node entry) {
            PathInfo pathInfo = new PathInfo();
            pathInfo.gp = null;
            pathInfo.p = entry;
            pathInfo.n = entry.nodes[0];
            pathInfo.nIdx = 0;

            while (!pathInfo.n.isLeaf()) {

                pathInfo.gp = pathInfo.p;
                pathInfo.p = pathInfo.n;
                pathInfo.pIdx = pathInfo.nIdx;
                pathInfo.nIdx = getChildIndex(pathInfo.n, low);
                pathInfo.n = pathInfo.n.nodes[pathInfo.nIdx];

            }
            Node leftNode = pathInfo.n;
            boolean continueToNextNode=true;
            while(true){
                for(int i=0;i<this.maxNodeSize;i++) {
                    ValueCell value = leftNode.values[i];
                    if(value == null){
                        continue;
                    }
                    if(value.key >= low && value.key <= high && value.insertionTime < TIMESTAMP.get()){
                        visit(threadId, value);

                        // System.out.println("Key: "+leftNode.keys[i]+ " Value: "+leftNode.values[i]);

                    }
                    if(leftNode.keys[i]>high) {
                        continueToNextNode = false;
                    }
                }
                if(continueToNextNode && leftNode.right != null) {

                    leftNode = leftNode.right;
                }
                else {
                    break;
                }
            }

        }

        private void initThread(int threadId) {
            if(this.threadsData[threadId] == null){
                this.threadsData[threadId] = new ThreadData(LIMBOLIST_SIZE);
            }
        }

        public void announcePhysicalDeletion(int threadId, ValueCell deletedKey) {
            initThread(threadId);
            this.threadsData[threadId].rqAnnouncements[threadsData[threadId].rqAnnouncementsSize] = deletedKey;
            threadsData[threadId].rqAnnouncementsSize++;
        }



        public void visit(int threadId, ValueCell value){
            tryAdd(threadId, value, null, RQSource.DataStructure);
        }

        public int traversalEnd(int threadId, int[] result){

            for(int i = 0; i<this.threadsDataSize; i++) {
                if(threadsData[i]==null){
                    continue;
                }
                for(int j=0;j<this.threadsData[i].rqAnnouncementsSize;j++)
                {
                    ValueCell announcement = threadsData[i].rqAnnouncements[j];
                    tryAdd(threadId,announcement, announcement, RQSource.Announcement);
                }

            }

            // Collect pointers to all limbo lists
            // Traverse limbo lists

            for(int j=0;j<this.threadsDataSize;j++) {
                if(threadsData[j] == null){
                    continue;
                }
                ValueCell[] limboList=threadsData[j].limboList;
                for(int i=0;i<LIMBOLIST_SIZE;i++){
                    if(limboList[i] == null){
                        break;
                    }
                    tryAdd(threadId, limboList[i], null, RQSource.LimboList);
                }
            }
            Arrays.sort(this.threadsData[threadId].result, new SortValueCells());

            int rangeSize=this.threadsData[threadId].rqHigh-this.threadsData[threadId].rqLow;
            for(int i=0;i<rangeSize;i++)
            {
                if(this.threadsData[threadId].result[i] == null)
                {
                    result[i] = 0;
                    continue;
                }
                result[i] = this.threadsData[threadId].result[i].value;
            }

            return this.threadsData[threadId].resultSize;
        }


        private void tryAdd(int threadId, ValueCell value, ValueCell announcedValue, RQSource rqSource) {
            int low = threadsData[threadId].rqLow;
            int high = threadsData[threadId].rqHigh;
            long rqLinearzationTime = threadsData[threadId].rqLinearzationTime;

            while (value.insertionTime == 0){}
            if(value.insertionTime >= rqLinearzationTime){
                return; // node inserted after RQ
            }
            if(rqSource == RQSource.DataStructure){
                // do nothing: node was not deleted when RQ was linearized
            }
            else if(rqSource == RQSource.LimboList){
                while (value.deletionTime == 0) {}
                if(value.deletionTime < rqLinearzationTime){
                    return; // node deleted before RQ
                }

            }
            else if(rqSource == RQSource.Announcement) {
                long deletionTime=0;
                while (deletionTime==0 && value == announcedValue) {
                    deletionTime= value.deletionTime;
                }

                if(deletionTime==0){
                    // loop exited because the process removed this announcement
                    // if the process deleted node, then it has now set node.dtime
                    deletionTime = value.deletionTime;

                    if(deletionTime == 0) {
                        // the process did not delete node,
                        // but another process might have
                        return;
                    }

                }
                if(deletionTime < rqLinearzationTime){
                    return; // node deleted before RQ
                }
            }
            if(value.key >= low && value.key <= high) {

                /*if(rqSource == RQSource.LimboList) {
                    rqResult.wasDeletedDuringRangeQuery = true;
                }*/

                /*if(threadsData[threadId].resultSize == 200){

                    Arrays.sort(threadsData[threadId].result,new SortKeyValues());
                    System.out.println("trouble");
                }*/
                if(rqSource == RQSource.LimboList || rqSource == RQSource.Announcement){
                    if(threadsData[threadId].vc_hashset.contains(value))
                    {
                        return;
                    }
                }

                threadsData[threadId].vc_hashset.add(value);
                threadsData[threadId].result[threadsData[threadId].resultSize++] = value;
            }
        }



        private void physicalDeletionSucceeded(int threadId, ValueCell deletedKey) {

            retire(threadId,deletedKey);
            // ensure nodes are placed in the epoch bag BEFORE they are removed from announcements.
            this.threadsData[threadId].rqAnnouncementsSize--;
        }

        private void retire(int threadId, ValueCell value) {
            ThreadData currentThreadData = this.threadsData[threadId];

            currentThreadData.limboList[currentThreadData.limboListCurrentIndex] = value;
            int nextIndex = currentThreadData.limboListCurrentIndex+1;
            currentThreadData.limboListCurrentIndex = nextIndex%LIMBOLIST_SIZE;

        }

        class RQResult {
            RQResult(int key, int value) {
                this.key = key;
                this.value = value;
            }
            int key;
            int value;

            public boolean wasDeletedDuringRangeQuery = false;
        }

    public int find(int key) {

        PathInfo pathInfo = new PathInfo();
        Result searchResult = search(key, null, pathInfo);

        if(searchResult.getReturnCode() != ReturnCode.SUCCESS){
            return NULL;
        }

        Node leaf = pathInfo.n;
        Result searchLeafResult = searchLeaf(leaf, key);

        return searchLeafResult.getValue();
    }


    public int tryInsert(int key, int value) {
        PathInfo pathInfo = new PathInfo();
        while (true) {
            Result searchResult = search(key,null,pathInfo);
            if(searchResult.getReturnCode() == ReturnCode.SUCCESS){
                return searchResult.getValue();
            }

            Result insertResult = insert(pathInfo,key,value);

            ReturnCode insertReturnCode = insertResult.getReturnCode();
            if (insertReturnCode == ReturnCode.SUCCESS || insertReturnCode == ReturnCode.FAILURE) {
                return insertResult.getValue();
            }

        }
    }

    public int tryDelete(int key) {
        PathInfo pathInfo = new PathInfo();
        while (true) {
            Result searchResult = search(key, null, pathInfo);

            if(searchResult.getReturnCode() == ReturnCode.FAILURE){
                return NULL;
            }

            Result deleteResult = delete(pathInfo, key);

            if(deleteResult.getReturnCode() == ReturnCode.SUCCESS || deleteResult.getReturnCode() == ReturnCode.FAILURE){
                return deleteResult.getValue();
            }
        }
    }

    public int scan(int[] result, int low, int high) {
        int threadId=((int) Thread.currentThread().getId());
        this.traversalStart(threadId,low,high,entry);

        var numberOfScannedKeys=traversalEnd(threadId,result);
        return numberOfScannedKeys;
    }
}
