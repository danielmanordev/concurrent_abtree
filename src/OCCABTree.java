

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class OCCABTree {

    /* Constants */
    private final int NULL = 0;
    public static int MAX_THREADS = 80;
    private Node entry;

    private final int minNodeSize;
    private final int maxNodeSize;

    //private final ThreadData[] threadsData;
    private final ScanData[] scanArray;
    //private final int threadsDataSize;



    //public  AtomicInteger GLOBAL_VERSION = new AtomicInteger(1);

    public OCCABTree(int a, int b) {
        this.minNodeSize = a;
        this.maxNodeSize = b;
        int anyKey = 26;
        Node entryLeft = createExternalNode(true,0,anyKey);
        entry = createInternalNode(true,1,anyKey);
        entry.nodes[0] = entryLeft;

        //this.threadsDataSize = (int)Math.pow(numberOfThreads+20,2);



        //this.threadsData = new ThreadData[threadsDataSize];
        this.scanArray = new ScanData[MAX_THREADS+1];

    }

    private boolean isParentOf(Node parent, Node child){
        /*if(child == null){
            return true;
        }
        for(int i=0;i<parent.size;i++){
            if(parent.nodes[i] != null){
                if(parent.nodes[i] == child){
                    return true;
                }
            }
        }*/
        return true;
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
        // PutData putData = new PutData(key,value);
        //node.publishPut(putData);
        int keyIdx =-1;
       for (int i = 0; i < this.maxNodeSize; ++i) {
            if (node.keys[i] == key) {
                //li = findLatest(key,Integer.MAX_VALUE,node);
                keyIdx = i;
                if((node.values[keyIdx].getLatestValue() == NULL && value != NULL) || (node.values[keyIdx].getLatestValue() != NULL && value == NULL)){
                    break;
                }

                //node.publishPut(null);
                node.unlock();
                return new Result(ReturnCode.FAILURE);
            }
        }

       if(keyIdx != -1){
           Result result = updateKeyInIndex(keyIdx,value,node);
           node.unlock();
           return result;
       }

        int currSize = node.size;
        if(currSize < this.maxNodeSize) {

          Result result = insertKey(key,value,node);
          node.unlock();
          return result;
        }
        else {

           int numberOfRemovedObsoleteKeys = cleanObsoleteKeys(node);

            // TODO: check if ordering of conditions below, between the diaz lines, can be optimized
            // ###########################################################
            if(numberOfRemovedObsoleteKeys > 0){
                Result writeResult = insertKey(key,value,node);
                node.unlock();
                fixUnderfull(node);
                return writeResult;
            }
            // ###########################################################

            parent.lock();
            boolean isRightMarked =false;
            boolean isLeftMarked =false;
            boolean isRightSameParent =true;
            boolean isLeftSameParent =true;

            if(parent.isMarked()){
                parent.unlock();
                node.unlock();
                return new Result(ReturnCode.RETRY);
            }
            isRightSameParent = isParentOf(parent,node.right);
            if(!isRightSameParent){
                   // node.right.lock();
                    isRightMarked = node.right.isMarked();
            }
            isLeftSameParent = isParentOf(parent,node.left);
            if(!isLeftSameParent){
                   // node.left.lock();
                    isLeftMarked = node.left.isMarked();
            }

            if(isRightMarked || isLeftMarked) {
                if(!isLeftSameParent){
                    //node.left.unlock();
                }

                if(!isRightSameParent){
                   // node.right.unlock();
                }
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
            var vc = new ValueCell(key);
            vc.putNewValue(value);
            keyValues[k] = new KeyValue(key, vc);
            keyValues[k].getValueCell().setLatestVersion(GlobalVersion.Value.get()); // no need to be atomic, at this point, only a single thread can access this
           // node.publishPut(null);
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
                left.values[i] = keyValues[i].getValueCell();

            }

            int rightSize = (this.maxNodeSize+1) - leftSize;
            Node right = createExternalNode(true,rightSize, keyValues[leftSize].getKey());
            for (int i = 0; i < rightSize; i++) {
                right.keys[i] = keyValues[i+leftSize].getKey();
                right.values[i] = keyValues[i+leftSize].getValueCell();

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

            if(!isLeftSameParent){
               // node.left.unlock();
            }

            if(!isRightSameParent){
               // node.right.unlock();
            }

            node.unlock();
            parent.unlock();
            fixTagged(replacementNode);
            return new Result(ReturnCode.SUCCESS);
        }
        // return new Result(ReturnCode.RETRY);

    }

   /*private Result writeToNode(int idx, int key, int value,Node node){
                int oldVersion = node.ver.get();
                node.ver.set(oldVersion+1);
                var vc = new ValueCell(key,value, System.currentTimeMillis());
                vc.version = 1;
                node.values[idx] = vc;
                node.keys[idx] = vc.key;
                node.ver.set(oldVersion+2);
                return new Result(node.values[idx].value,ReturnCode.SUCCESS);
    }*/

    private Result updateKeyInIndex(int keyIndex, int value, Node node){
        node.version++;
        var vc = node.values[keyIndex];
        vc.putNewValue(value);
        vc.casLatestVersion(0, GlobalVersion.Value.get());
        node.version++;
        return new Result(node.values[keyIndex].getLatestValue(),ReturnCode.SUCCESS);
    }

    private Result insertKey(int key, int value, Node node){
        for (int i = 0; i < this.maxNodeSize; ++i) {
            if (node.keys[i] == NULL) {
                node.version++;

                var vc = new ValueCell(key);
                vc.putNewValue(value);
                node.values[i] = vc;
                node.keys[i] = vc.key;
                node.values[i].casLatestVersion(0, GlobalVersion.Value.get());
                node.size++;
                // node.setLatestVersion(vc.key,vc, i);
                node.version++;
                return new Result(node.values[i].getLatestValue(),ReturnCode.SUCCESS);
            }
        }
        return new Result(value,ReturnCode.RETRY);
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

    // TODO: find latest
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


    /*KeyIndexValueVersionResult getKeyIndexValueVersion(Node node, int key) {

        ValueCell value;
        int version;

        do {
            while (((version = node.ver.get()) & 1) != 0) {}
            int latestIndex = findLatest(key,Integer.MAX_VALUE,node);

            value = latestIndex != -1 ? node.values[latestIndex] : null;
        } while (node.ver.get() != version);
        if(value == null) {
            return new KeyIndexValueVersionResult(NULL,NULL,ReturnCode.FAILURE);
        }
        else if(value.value == NULL){
            return new KeyIndexValueVersionResult(NULL,NULL,ReturnCode.FAILURE);
        }
        else {
            return new KeyIndexValueVersionResult(value.value,version,ReturnCode.SUCCESS);
        }
    }*/

    /*KeyIndexValueVersionResult getKeyIndexValueVersion(Node node, int key) {
        int keyIndex, latestIndex,latestVersion;
        ValueCell value;
        int version;

        do {
            while (((version = node.ver.get()) & 1) != 0) {}
            keyIndex = 0;
            latestIndex=-1;
            latestVersion=0;
            while (keyIndex < this.maxNodeSize) {
                if(node.keys[keyIndex] == key){
                    var vc = node.values[keyIndex];
                    if(vc == null){
                        continue;
                    }
                    if(vc.key != key){
                        continue;
                    }
                    if(vc.version > latestVersion){
                        latestIndex = keyIndex;
                    }
                }
                ++keyIndex;
            }

            value = latestIndex != -1 ? node.values[latestIndex] : null;
        } while (node.ver.get() != version);
        return value == null ? new KeyIndexValueVersionResult(NULL,NULL,ReturnCode.FAILURE) : new KeyIndexValueVersionResult(value.value,version,ReturnCode.SUCCESS);

    }*/

    KeyIndexValueVersionResult getKeyIndexValueVersion(Node node, int key) {
        int keyIndex;
        ValueCell value = null;
        int version;

        do {
            while (((version = node.version) & 1) != 0) {}
            keyIndex = 0;
            while (keyIndex < this.maxNodeSize && node.keys[keyIndex] != key) {
                ++keyIndex;
            }
            if(keyIndex < this.maxNodeSize) {
                var vc = node.values[keyIndex];
                if (vc == null) {
                    keyIndex--;
                    continue;
                }
                if (vc.key != key) {
                    keyIndex--;
                    continue;
                }
                value = vc;
            }
        } while (node.version != version);
        return value == null ? new KeyIndexValueVersionResult(NULL,NULL,ReturnCode.FAILURE) : new KeyIndexValueVersionResult(value.getLatestValue(),version,ReturnCode.SUCCESS);

    }


    private int getChildIndex(Node node, int key) {
        int numberOfKeys = getKeyCount(node);
        int retval = 0;

        while (retval < numberOfKeys && key >= node.keys[retval]) {
            ++retval;
        }
        return retval;

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
               boolean isRightSameParent=false,isLeftSameParent=false,isLeftMarked=false,isRightMarked=false;
               // create new node(s))
               int keyCounter = 0, ptrCounter = 0;
               if (left.isLeaf()) {

                   isRightSameParent = isParentOf(parent,right.right);
                   if(!isRightSameParent){
                      // right.right.lock();
                       isRightMarked = right.right.isMarked();
                   }

                   isLeftSameParent = isParentOf(parent,left.left);
                   if(!isLeftSameParent){
                //       left.left.lock();
                       isLeftMarked = left.left.isMarked();
                   }

                   if(isRightMarked || isLeftMarked){
                       if(!isRightSameParent){
                      //     right.right.unlock();
                       }
                       if(!isLeftSameParent){
                         //  left.left.unlock();
                       }

                       node.unlock();
                       sibling.unlock();
                       parent.unlock();
                       gParent.unlock();
                       continue;
                   }

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

                   newNodeExt.right = right.right;
                   if(right.right != null) {
                       right.right.left = newNodeExt;
                   }

                   newNodeExt.left = left.left;
                   if(left.left != null) {
                       left.left.right = newNodeExt;
                   }

                   if(!isRightSameParent){
                     //  right.right.unlock();
                   }

                   if(!isLeftSameParent){
                    //   left.left.unlock();
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
                           keyValues[valCounter++].valueCell = left.values[i];

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
                          keyValues[valCounter++].valueCell = right.values[i];

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
               boolean isRightSameParent=false,isLeftSameParent=false,isLeftMarked=false,isRightMarked=false;
               if (left.isLeaf()) {


                   isRightSameParent = isParentOf(parent,right.right);
                   if(!isRightSameParent){
                     //  right.right.lock();
                       isRightMarked = right.right.isMarked();
                   }

                   isLeftSameParent = isParentOf(parent,left.left);
                   if(!isLeftSameParent){
                      // left.left.lock();
                       isLeftMarked = left.left.isMarked();
                   }

                   if(isRightMarked || isLeftMarked){
                       if(!isRightSameParent){
                         //  right.right.unlock();
                       }
                       if(!isLeftSameParent){
                        //   left.left.unlock();
                       }

                       node.unlock();
                       sibling.unlock();
                       parent.unlock();
                       gParent.unlock();
                       continue;
                   }

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
                       newLeftExt.values[i] = keyValues[valCounter++].valueCell;

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
                       newRight.values[i] = keyValues[valCounter++].valueCell;
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
               if(!isRightSameParent){
                 //  right.right.unlock();
               }

               if(!isLeftSameParent){
                 //  left.left.unlock();
               }
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
            int ver1 = leaf.version;
            if (ver1 % 2 != 0) {
                continue;
            }

            int val = NULL;
            for (int keyIndex = 0; keyIndex < this.maxNodeSize; keyIndex++) {
                if (leaf.keys[keyIndex] == key) {
                    val = leaf.values[keyIndex].getLatestValue();
                    break;
                }
            }
            int ver2 = leaf.version;
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
/*
    private Result searchLeaf(Node leaf, int key) {
        if(!leaf.isLeaf()) {
            return new Result(NULL, ReturnCode.FAILURE);
        }
        while (true) {
            int ver1 = leaf.ver.get();
            if (ver1 % 2 != 0) {
                continue;
            }

            // int latestIndex = findLatest(key, Integer.MAX_VALUE,leaf);
            ValueCell value = leaf.values[latestIndex];
            int ver2 = leaf.ver.get();
            if (ver1 != ver2) {
                continue;
            }
            
            if (value == null) {
                return new Result(NULL, ReturnCode.FAILURE);
            } else {
                return new Result(value.getLatestValue(), ReturnCode.SUCCESS);
            }
        }
    }
*/
        // TODO: continue here
        public int traversalStart(int low, int high, Node entry, int[] result) {
            int myVer = newVersion(low,high);
            int resultSize =0;


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
            KeyValue[] kvs = new KeyValue[maxNodeSize];
            var sortKvs = new SortKeyValues();
            int kvsSize;

            while(true){
                kvsSize=0;

                for(int i=0;i<this.maxNodeSize;i++) {
                    int key = leftNode.keys[i];
                    if(key == 0){
                        continue;
                    }
                    ValueCell valueCell = leftNode.values[i];
                    if(valueCell == null){
                        continue;
                    }

                    if(key >= low && key <= high) {
                        int value = valueCell.helpAndGetValueByVersion(myVer);

                        if(value == 0){
                           continue;
                        }
                        kvs[kvsSize] = new KeyValue(key,value);
                        kvsSize++;
                    }
                    if(key>high) {
                        continueToNextNode = false;
                    }
                }
                Arrays.sort(kvs,0,kvsSize,sortKvs);

                for(int i=0;i<kvsSize;++i){
                    // i = findLatestSorted(kvs[i].key, myVer,kvs,i,kvsSize);
                    result[resultSize]=kvs[i].value;
                    resultSize++;
                }
                if(continueToNextNode && leftNode.right != null) {
                    leftNode = leftNode.right;
                }
                else {
                    break;
                }

            }
            publishScan(null);
            return resultSize;
        }

        private int newVersion(int low, int high){
           ScanData scanData = new ScanData(low,high);
           publishScan(scanData);
           int myVer = GlobalVersion.Value.getAndIncrement();

            if(scanData.version.compareAndSet(0, myVer))
                return myVer;
            else
                return scanData.version.get();

        }

    private void publishScan(ScanData scanData)
    {
        int idx = (int) (Thread.currentThread().getId() % MAX_THREADS);

        // publish into thread array
        scanArray[idx] = scanData;
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

            if((searchResult.getReturnCode() == ReturnCode.SUCCESS && value != NULL) || (searchResult.getReturnCode() == ReturnCode.FAILURE && value == NULL)){
                return searchResult.getValue();
            }

            Result insertResult = insert(pathInfo,key,value);

            ReturnCode insertReturnCode = insertResult.getReturnCode();
            if (insertReturnCode == ReturnCode.SUCCESS || insertReturnCode == ReturnCode.FAILURE) {
                return insertResult.getValue();
            }

        }
    }

    // TODO: Update latest key Hashmap after deleteing keys
    private int cleanObsoleteKeys(Node node) {

        // get smallest on-going range version
        int minVersion=Integer.MAX_VALUE;
        int numberOfCleanedKeys=0;
        for(int i=0; i < OCCABTree.MAX_THREADS; ++i){
            ScanData scanData = this.scanArray[i];
            if(scanData == null){
                continue;
            }
            int scanDataVersion=scanData.version.get();

            if(scanDataVersion == 0){
                int newVersion = GlobalVersion.Value.getAndIncrement();
                if(scanData.version.compareAndSet(0,newVersion)){
                    scanDataVersion = newVersion;
                }
                else{
                    scanDataVersion=scanData.version.get();
                }
            }

            if(scanDataVersion < minVersion){
                minVersion=scanDataVersion;
            }
        }

        for (int i=0;i<this.maxNodeSize;++i)
        {
            int key=node.keys[i];
            var vc = node.values[i];
            var latestValue = vc.helpAndGetValueByVersion(Integer.MAX_VALUE);
            if(latestValue != 0){
               continue;
            }
            // key is deleted, lets see if we can remove it and save space
            var latestVersion = vc.getLatestVersion();
            if(minVersion >= latestVersion){
                node.version++;
                node.keys[i] = 0;
                node.values[i] = null;
                node.size--;
                node.version++;
                numberOfCleanedKeys++;
            }
        }
        return numberOfCleanedKeys;
    }


    public int scan(int[] result, int low, int high) {
        return this.traversalStart(low,high,entry, result);
    }

}
