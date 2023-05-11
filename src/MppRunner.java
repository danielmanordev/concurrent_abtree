import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MppRunner {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        int dataRange = 10000;
        int numberOfThreads = 4;
        int a = 2;
        int b = 4;
        int numberOfTests = 8;
        for (int i = 0; i < numberOfTests; i++) {

            Set concurrentSet = new OCCABTree(a, b);
            TestSet.seed(concurrentSet, dataRange, dataRange / 2);

            //nteger.parseInt(args[1]);

            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(concurrentSet, numberOfThreads, dataRange, 0, 100, 10000);
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            long timeElapsedMicroseconds = timeElapsed * 1000;
            System.out.println();
            System.out.println("TEST "+(i+1)+"/"+numberOfTests+" FINISHED - RESULTS");
            System.out.println("*****************************************");
            System.out.println("a:                        " + a);
            System.out.println("b:                        " + b);
            System.out.println("Total adds:               " + testResult.TotalAdds.longValue());
            System.out.println("Total removes:            " + testResult.TotalRemoves.longValue());
            System.out.println("Total contains:           " + testResult.TotalContains.longValue());
            System.out.println("Adds per microsecond:     " + testResult.TotalAdds.longValue() / (timeElapsedMicroseconds));
            System.out.println("Removes per microsecond:  " + testResult.TotalRemoves.longValue() / (timeElapsedMicroseconds));
            System.out.println("Contains per microsecond: " + testResult.TotalContains.longValue() / (timeElapsedMicroseconds));
            System.out.println("Threads:                  " + numberOfThreads);
            System.out.println("Total time:               " + timeElapsed + " milliseconds");
            b *=2;
        }
    }
}
