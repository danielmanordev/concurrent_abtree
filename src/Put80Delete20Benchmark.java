import abstractions.Set;
import abstractions.SetFactory;
import benchmark.Test;

import java.util.ArrayList;

public class Put80Delete20Benchmark implements Test {
    private Set set;
    public Put80Delete20Benchmark(Set set){
        this.set = set;
    }
    @Override
    public void run() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        int dataRange = 1000000;
        int numberOfThreads = 1;
        int numberOfScanThreads = 0;//Integer.parseInt(args[0]);
        int numberOfTests = 8;
        int testDuration=10000;
        int perAdd=80;
        int perContains=0;
        int perRemove=20;
        /// int perRange=100-perAdd-perContains-perRemove;
        ArrayList<Long> adds = new ArrayList();
        System.out.println("Number of available processors: "+availableProcessors);
        System.out.println("Number of tests: "+numberOfTests);
        System.out.println("Dataset Size: "+dataRange+" keys");
        System.out.println("Single test duration: "+testDuration+ " ms");
        System.out.println("insert: "+perAdd+"%");
        System.out.println("remove: "+perRemove+"%");
        System.out.println("contains: "+perContains+"%");
        System.out.println("Starting, 80% insert, 20% delete....");
        Set concurrentSet = set;
        for (int i = 0; i < numberOfTests; i++) {


            TestSet.seed(concurrentSet, dataRange, dataRange / 2);
            TestResult testResult = TestSet.runTest(concurrentSet, numberOfThreads, numberOfScanThreads ,dataRange, perContains, perAdd,1,32000,testDuration,false);
            System.out.println("("+numberOfThreads +","+ testResult.TotalAdds.longValue()+")" + ":("+numberOfThreads +","+ testResult.TotalRemoves.longValue()+")");

            numberOfThreads++;
            concurrentSet = ((SetFactory)set).newInstance();

        }
    }
}
