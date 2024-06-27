import abstractions.Set;
import benchmark.JavaConcurrentSkipList;

import java.util.ArrayList;

public class ScanOnlyBenchmark {
    public static void main(String[] args) {
      System.out.println("SCAN ONLY");
      System.out.println("JavaConcurrentSkipList");
   //   run(new JavaConcurrentSkipList());
      System.out.println("MTASet");
      run(new MTASet(2,256));
    }
    private static void run(Set cs){
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int dataRange = 1000000;
        int numberOfThreads = 1;
        int numberOfScanThreads = 1;//Integer.parseInt(args[0]);
        int numberOfTests = 80;
        int testDuration=10000;
        int perAdd=0;
        int perContains=0;
        int perRemove=0;
        boolean scanOnly=true;
        /// int perRange=100-perAdd-perContains-perRemove;

        System.out.println("Number of available processors: "+availableProcessors);
        System.out.println("Number of tests: "+numberOfTests);
        System.out.println("Dataset Size: "+dataRange+" keys");
        System.out.println("Single test duration: "+testDuration+ " ms");
        System.out.println("insert: "+perAdd+"%");
        System.out.println("remove: "+perRemove+"%");
        System.out.println("contains: "+perContains+"%");
        System.out.println("Starting....");
        boolean wu=true;
        for (int i = 0; i < numberOfTests; i++) {

            TestSet.fill(cs,dataRange);

            if(wu){
                System.out.println("Warming up...");
                TestResult testResult = TestSet.runTest(cs, numberOfThreads, numberOfScanThreads ,dataRange, perContains, perAdd,1,32000,testDuration,false);
                System.out.println("Warming up done");
                wu = false;
                i--;
                continue;
            }

            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(cs, numberOfThreads, numberOfScanThreads ,dataRange, perContains, perAdd,1,32000,testDuration,scanOnly);
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            //long timeElapsedMicroseconds = timeElapsed * 1000;
            //System.out.println();
            //System.out.println("TEST " + (i + 1) + "/" + numberOfTests + " FINISHED - RESULTS");
            //System.out.println("*****************************************");
            //System.out.println("Total adds:               " + testResult.TotalAdds.longValue());
            //System.out.println("Total removes:            " + testResult.TotalRemoves.longValue());
            //System.out.println("Total contains:           " + testResult.TotalContains.longValue());
            System.out.println(numberOfScanThreads +":"+ testResult.numberOfScannedKeys.longValue());
            //System.out.println("Adds/\u33B2:                  " + testResult.TotalAdds.doubleValue() / (timeElapsedMicroseconds));
            //System.out.println("Removes/\u33B2:               " + testResult.TotalRemoves.doubleValue() / (timeElapsedMicroseconds));
            //System.out.println("Contains/\u33B2:              " + testResult.TotalContains.doubleValue() / (timeElapsedMicroseconds));
            //System.out.println("Total Threads:            " + numberOfThreads);
            //System.out.println("Scan Threads:             " + numberOfScanThreads);
            //System.out.println("Non Scan Threads:         " + (numberOfThreads-numberOfScanThreads));
            //System.out.println("Total time:               " + timeElapsed + " milliseconds");
            numberOfThreads++;
            numberOfScanThreads++;

        }
    }
}
