import abstractions.Set;

import java.util.ArrayList;

public class MppRunner2 {

    public static void main(String[] args) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int[] res = new int[1000];
            Set concurrentSet = new MTASet(2,4);
            concurrentSet.add(1,1);
            concurrentSet.add(3,3);
            concurrentSet.add(2,2);
            concurrentSet.add(4,4);

            concurrentSet.getRange(res,0,1000);
            concurrentSet.remove(3);
            concurrentSet.getRange(res,0,1000);
            concurrentSet.add(3,30);
            concurrentSet.getRange(res,0,1000);
            concurrentSet.remove(3);
            concurrentSet.getRange(res,0,1000);
            concurrentSet.add(5,55);
        int[] res2 = new int[1000];
        concurrentSet.getRange(res2,0,1000);
        System.out.println(123);

            }
        }

