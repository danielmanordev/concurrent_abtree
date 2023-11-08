import abstractions.Set;

import java.util.ArrayList;

public class MppRunner {

    public static void main(String[] args) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        int dataRange = 1000000;
        int numberOfThreads = 12;
        int numberOfScanThreads = 6;//Integer.parseInt(args[0]);
        int a = 2;
        int b = 16;
        int numberOfTests = 4;
        int testDuration=10000;
        int perAdd=100;
        int perContains=0;
        int perRemove=0;
        /// int perRange=100-perAdd-perContains-perRemove;
        ArrayList<Long> adds = new ArrayList();
        System.out.println("Number of available processors: "+availableProcessors);
        System.out.println("Number of tests: "+numberOfTests);
        System.out.println("Dataset Size: "+dataRange+" keys");
        System.out.println("Single test duration: "+testDuration+ " ms");
        System.out.println("insert: "+perAdd+"%");
        System.out.println("remove: "+perRemove+"%");
        System.out.println("contains: "+perContains+"%");
        System.out.println("Starting....");
        for (int i = 0; i < numberOfTests; i++) {

            Set concurrentSet = new MTASet(a,b,numberOfThreads);
            TestSet.seed(concurrentSet, dataRange, dataRange / 2);

            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(concurrentSet, numberOfThreads, numberOfScanThreads ,dataRange, perContains, perAdd,1,32000,testDuration);
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            long timeElapsedMicroseconds = timeElapsed * 1000;
            System.out.println();
            System.out.println("TEST " + (i + 1) + "/" + numberOfTests + " FINISHED - RESULTS");
            System.out.println("*****************************************");
            System.out.println("a:                        " + a);
            System.out.println("b:                        " + b);
            System.out.println("Total adds:               " + testResult.TotalAdds.longValue());
            System.out.println("Total removes:            " + testResult.TotalRemoves.longValue());
            System.out.println("Total contains:           " + testResult.TotalContains.longValue());
            System.out.println("Number of scanned keys:   " + testResult.numberOfScannedKeys.longValue());
            System.out.println("Adds/\u33B2:                  " + testResult.TotalAdds.doubleValue() / (timeElapsedMicroseconds));
            System.out.println("Removes/\u33B2:               " + testResult.TotalRemoves.doubleValue() / (timeElapsedMicroseconds));
            System.out.println("Contains/\u33B2:              " + testResult.TotalContains.doubleValue() / (timeElapsedMicroseconds));
            System.out.println("Total Threads:            " + numberOfThreads);
            System.out.println("Scan Threads:             " + numberOfScanThreads);
            System.out.println("Non Scan Threads:         " + (numberOfThreads-numberOfScanThreads));
            System.out.println("Total time:               " + timeElapsed + " milliseconds");
            //numberOfThreads++;
            if (i > 1) {
                adds.add(testResult.TotalAdds.longValue());
            }
        }
    }
}
