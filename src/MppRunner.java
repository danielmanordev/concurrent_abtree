import java.io.IOException;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutionException;

public class MppRunner {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();

        int dataRange = 10000;
        int numberOfThreads = 8;
        int a = 2;
        int b = 16;
        int numberOfTests = 96;
        int testDuration=10000;
        int perAdd=80;
        int perContains=0;
        int perDelete=20;
        int perRemove=100-perAdd-perContains;
        int perRange=0;
        ArrayList<Long> adds = new ArrayList();
        System.out.println("Number of cores: "+cores);
        System.out.println("Number of tests: "+numberOfTests);
        System.out.println("Single test duration: "+testDuration+ " ms");
        System.out.println("insert: "+perAdd+"%");
        System.out.println("remove: "+perRemove+"%");
        System.out.println("contains: "+perContains+"%");
        System.out.println("range: "+perRange+"%");
        System.out.println("Starting....");
        for (int i = 0; i < numberOfTests; i++) {

            Set concurrentSet = new OCCABTree(a, b, numberOfThreads);
            TestSet.seed(concurrentSet, dataRange, dataRange / 2);

            //nteger.parseInt(args[1]);

            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(concurrentSet, numberOfThreads, dataRange, perContains,perAdd ,perDelete, testDuration);
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
            System.out.println("Adds/\u33B2:                  " + testResult.TotalAdds.longValue() / (timeElapsedMicroseconds));
            System.out.println("Removes/\u33B2:               " + testResult.TotalRemoves.doubleValue() / (timeElapsedMicroseconds));
            System.out.println("Contains/\u33B2:              " + testResult.TotalContains.longValue() / (timeElapsedMicroseconds));
            System.out.println("Threads:                  " + numberOfThreads);
            System.out.println("Total time:               " + timeElapsed + " milliseconds");
            // numberOfThreads++;
            if(i >1) {
                adds.add(testResult.TotalAdds.longValue());
            }
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
