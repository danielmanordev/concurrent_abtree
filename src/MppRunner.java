import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MppRunner {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {/*Set set = new OCCABTree(2,Constants.DEGREE);
        set.add(1,1);
        set.add(2,1);
        set.add(3,1);
        set.add(4,1);
        set.add(5,1);
        set.add(6,1);
        set.add(7,1);
        set.add(8,1);
        set.add(9,1);
        set.add(10,1);
        set.add(11,1);
        set.add(12,1);
        set.add(13,1);
        set.add(14,1);
        set.add(15,1);
        set.add(16,1);
        set.add(17,1);
        set.add(18,1);
        set.remove(1);
        set.remove(2);
        set.remove(3);
        set.remove(4);
        set.remove(5);
        set.remove(6);
        set.remove(7);
        set.remove(8);
        set.remove(9);
        set.remove(11);
        set.remove(11);
        set.remove(12);
        set.remove(13);
        set.remove(14);
        set.remove(15);
        set.remove(16);
        set.remove(17);
        set.remove(18);





        boolean res = set.contains(19);
        boolean res2 = set.contains(2);
       */

        for (int i = 1; i <= 10; i++) {
            int numberOfThreads =18;//nteger.parseInt(args[1]);
            Set concurrentSet = new OCCABTree(2, Constants.DEGREE);


            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(concurrentSet, numberOfThreads, 10000, 0, 100, 10000);
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            long timeElapsedMicroseconds = timeElapsed*1000;
            System.out.println("Adds per microsecond: "+ testResult.TotalAdds.longValue()/(timeElapsedMicroseconds));
            System.out.println("Removes per microsecond: "+ testResult.TotalRemoves.longValue()/(timeElapsedMicroseconds));
            System.out.println("Contains per microsecond: "+ testResult.TotalContains.longValue()/(timeElapsedMicroseconds));
            System.out.println("Total time: " + timeElapsed + " milliseconds with " + numberOfThreads + " threads");

        }
    }
     }
