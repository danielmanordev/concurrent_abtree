import abstractions.Set;
import abstractions.SetFactory;
import benchmark.JavaConcurrentSkipList;
import benchmark.Test;

import java.util.ArrayList;

public class ScanOnlyBenchmark implements Test {
    Set set;
    int maxNumberOfScanThreads;

    public ScanOnlyBenchmark(Set set, int maxNumberOfScanThreads){
        this.set = set;
        this.maxNumberOfScanThreads = maxNumberOfScanThreads;
    }

     public void run(){
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int dataRange = 1000000;
        int numberOfThreads = 1;
        int numberOfScanThreads = 1;//Integer.parseInt(args[0]);
        int numberOfTests = maxNumberOfScanThreads;
        int testDuration=10000;

        System.out.println("Number of available processors: "+availableProcessors);
        System.out.println("Number of tests: "+numberOfTests);
        System.out.println("Dataset Size: "+dataRange+" keys");
        System.out.println("Single test duration: "+testDuration+ " ms");
        System.out.println("name: "+((SetFactory)set).getName());
        System.out.println("Scan Only, Starting....");


        for (int i = 0; i < numberOfTests; i++) {

            TestSet.fill(set,64000);
            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(set, numberOfThreads, numberOfScanThreads ,dataRange, 0, 0,1,32000,testDuration,true);
            long finish = System.currentTimeMillis();
            System.out.println("("+numberOfScanThreads +" "+ testResult.numberOfScannedKeys.longValue()+")");
            numberOfThreads++;
            numberOfScanThreads++;
            this.set = ((SetFactory)set).newInstance();
        }


        }

}
