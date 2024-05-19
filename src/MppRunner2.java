import abstractions.Set;

import java.util.ArrayList;

public class MppRunner2 {

    public static void main(String[] args) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

            Set concurrentSet = new MTASet(2,4,8);
            concurrentSet.add(3,3);
            concurrentSet.add(2,2);
            concurrentSet.add(1,1);
        concurrentSet.add(10,10);
        concurrentSet.add(100,100);
        concurrentSet.add(1000,1000);
        concurrentSet.add(4,4);
        concurrentSet.add(5,5);
        concurrentSet.remove(5);
        concurrentSet.add(5,6);
        concurrentSet.remove(5);
        concurrentSet.add(5,55);
        int[] res = new int[1000];
        concurrentSet.getRange(res,0,1000);
        System.out.println(123);

            }
        }

