import abstractions.Set;
import abstractions.SetFactory;
import benchmark.Test;

public class WarmupBenchmark implements Test {
    Set set;
    int insertPercent,deletePercent,numberOfScanThreads,numberOfThreads;
    public WarmupBenchmark(Set set, int insertPercent, int deletePercent, int numberOfScanThreads,int numberOfThreads){
        this.deletePercent = deletePercent;
        this.insertPercent = insertPercent;
        this.numberOfThreads = numberOfThreads;
        this.set = set;
        this.numberOfScanThreads = numberOfScanThreads;
    }
    @Override
    public void run(){
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int dataRange = 1000000;
        int numberOfThreads = this.numberOfThreads;
        int numberOfScanThreads = this.numberOfScanThreads;//Integer.parseInt(args[0]);
        int numberOfTests = 1;
        int testDuration=10000;
        int perAdd=this.insertPercent;
        int perContains=0;
        int perRemove=this.deletePercent;
        boolean scanOnly=false;
        /// int perRange=100-perAdd-perContains-perRemove;

        System.out.println("Number of available processors: "+availableProcessors);
        System.out.println("Number of tests: "+numberOfTests);
        System.out.println("Dataset Size: "+dataRange+" keys");
        System.out.println("Single test duration: "+testDuration+ " ms");
        System.out.println("insert: "+perAdd+"%");
        System.out.println("remove: "+perRemove+"%");
        System.out.println("contains: "+perContains+"%");
        System.out.println("name: "+((SetFactory)set).getName());
        System.out.println("WARM UP, Starting....");


        for (int i = 0; i < numberOfTests; i++) {

            TestSet.seed(set,dataRange,dataRange/2);
            long start = System.currentTimeMillis();
            TestResult testResult = TestSet.runTest(set, numberOfThreads, numberOfScanThreads ,dataRange, perContains, perAdd,1,32000,testDuration,scanOnly);
            long finish = System.currentTimeMillis();
            System.out.println("("+numberOfScanThreads +","+ testResult.numberOfScannedKeys.longValue()+")");
            //numberOfThreads++;
            numberOfScanThreads++;
            this.set = ((SetFactory)set).newInstance();
        }




    }
}
