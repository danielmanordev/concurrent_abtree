import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MppRunner {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        int dataRange = 10000;


            Set concurrentSet = new OCCABTree(2, Constants.DEGREE);
            TestSet.seed(concurrentSet,dataRange,dataRange/2);

            int numberOfThreads =4;//nteger.parseInt(args[1]);

            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(concurrentSet, numberOfThreads, dataRange, 0, 100, 10000);
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            long timeElapsedMicroseconds = timeElapsed*1000;
            System.out.println("Adds per microsecond: "+ testResult.TotalAdds.longValue()/(timeElapsedMicroseconds));
            System.out.println("Removes per microsecond: "+ testResult.TotalRemoves.longValue()/(timeElapsedMicroseconds));
            System.out.println("Contains per microsecond: "+ testResult.TotalContains.longValue()/(timeElapsedMicroseconds));
            System.out.println("Total time: " + timeElapsed + " milliseconds with " + numberOfThreads + " threads");
    }
     }
