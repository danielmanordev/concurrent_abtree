import abstractions.Set;
import abstractions.SetFactory;
import benchmark.Test;

import java.util.ArrayList;

public class InsertDeleteGetBenchmark implements Test {
    private Set set;
    private int maxNumberOfThreads,percentInsert,percentDelete,percentGet;
    public InsertDeleteGetBenchmark(Set set, int percentInsert, int percentDelete,int percentGet, int maxNumberOfThreads){
        this.set = set;
        this.maxNumberOfThreads = maxNumberOfThreads;
        this.percentInsert = percentInsert;
        this.percentDelete = percentDelete;
        this.percentGet = percentGet;
    }
    @Override
    public void run() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        int dataRange = 1000000;
        int numberOfThreads = 1;
        int numberOfScanThreads = 0;//Integer.parseInt(args[0]);
        int numberOfTests = this.maxNumberOfThreads;
        int testDuration=10000;
        int perAdd=percentInsert;
        int perContains=percentGet;
        int perRemove=100-(percentInsert+percentGet);

        System.out.println("Number of available processors: "+availableProcessors);
        System.out.println("Number of tests: "+numberOfTests);
        System.out.println("Dataset Size: "+dataRange+" keys");
        System.out.println("Single test duration: "+testDuration+ " ms");
        System.out.println("name: "+((SetFactory)set).getName());
        System.out.println("Starting, "+ perAdd+"% insert, "+perRemove+"% delete "+perContains+"% get");
        Set concurrentSet = set;
        for (int i = 0; i < numberOfTests; i++) {


            TestSet.seed(concurrentSet, dataRange, dataRange / 2);
            TestResult testResult = TestSet.runTest(concurrentSet, numberOfThreads, numberOfScanThreads ,dataRange, perContains, perAdd,1,32000,testDuration,false);
            System.out.println("("+numberOfThreads +","+ testResult.TotalAdds.longValue()+")" + ":("+numberOfThreads +","+ testResult.TotalRemoves.longValue()+")"+":("+numberOfThreads +","+ testResult.TotalContains.longValue()+")");

            numberOfThreads++;
            concurrentSet = ((SetFactory)set).newInstance();

        }
    }
}
