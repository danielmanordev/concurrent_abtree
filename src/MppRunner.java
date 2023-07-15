import java.io.IOException;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutionException;

public class MppRunner {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        int dataRange = 10000;
        int numberOfThreads = 1;
        int a = 2;
        int b = 16;
        int numberOfTests = 3;
        ArrayList<Long> adds = new ArrayList();
        for (int i = 0; i < numberOfTests; i++) {

            Set concurrentSet = new OCCABTree(a, b, numberOfThreads);
            TestSet.seed(concurrentSet, dataRange, dataRange / 2);
            concurrentSet.scan(1, 1000);

            //nteger.parseInt(args[1]);

            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(concurrentSet, numberOfThreads, dataRange, 0, 100, 0,10000);
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
            System.out.println("Total scans:              " + testResult.TotalScans.longValue());
            System.out.println("Adds/\u33B2:                  " + testResult.TotalAdds.longValue() / (timeElapsedMicroseconds));
            System.out.println("Removes/\u33B2:               " + testResult.TotalRemoves.doubleValue() / (timeElapsedMicroseconds));
            System.out.println("Contains/\u33B2:              " + testResult.TotalContains.longValue() / (timeElapsedMicroseconds));
            System.out.println("Scans/\u33B2:                 " + testResult.TotalScans.longValue() / (timeElapsedMicroseconds));
            System.out.println("Threads:                  " + numberOfThreads);
            System.out.println("Total time:               " + timeElapsed + " milliseconds");
           // b *=2;
            if(i >1) {
                adds.add(testResult.TotalAdds.longValue());
            }
         //   concurrentSet.scan(1,1000);
        }

        if(numberOfTests > 2){
            double average = adds
                    .stream()
                    .mapToDouble(n -> n)
                    .average().orElse(0.0);
            average /= 1000;
            int numberOfCalcTests = adds.size();
            System.out.println("Adds/\u33B2 last " +numberOfCalcTests+ " tests average:  " + average);
        }
    }
}
