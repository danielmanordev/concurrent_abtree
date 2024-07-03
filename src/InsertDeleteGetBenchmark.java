import abstractions.Set;
import abstractions.SetFactory;
import benchmark.Test;

import java.util.ArrayList;

public class InsertDeleteGetBenchmark implements Test {
    private Set set;
    private int numberOfTests,percentInsert,percentDelete,percentGet;
    public InsertDeleteGetBenchmark(Set set, int percentInsert, int percentDelete,int percentGet, int numberOfTests){
        this.set = set;
        this.numberOfTests = numberOfTests;
        this.percentInsert = percentInsert;
        this.percentDelete = percentDelete;
        this.percentGet = percentGet;
    }
    @Override
    public void run() {

        int availableProcessors = Runtime.getRuntime().availableProcessors();

        int dataRange = 1000000;
        int numberOfThreads = 1;
        int testDuration=10000;
        int perAdd=percentInsert;
        int perContains=percentGet;
        int perRemove=100-(percentInsert+percentGet);


        System.out.println("Starting, "+ perAdd+"% insert, "+perRemove+"% delete "+perContains+"% get");
        for (int i = 0; i < this.numberOfTests; i++) {
            double ratePerSec =0.0;
            for (int j=0;j<10;j++){
                TestSet.seed(set,dataRange,dataRange/2);
                TestResult testResult = TestSet.runTest(set, numberOfThreads, 0 ,dataRange, perContains, perAdd,1,32000,testDuration,false);
                double perSec = (testResult.TotalContains.longValue()/10);
               /* System.out.println(testResult.TotalAdds);
                System.out.println(testResult.TotalRemoves);
                System.out.println(testResult.TotalContains);*/
                ratePerSec += (perSec/1000000);
                this.set = ((SetFactory)set).newInstance();

            }
            System.out.print("("+numberOfThreads +","+ ratePerSec/10 +") ");
            //numberOfThreads++;
            numberOfThreads*=2;

            //System.out.println(((SetFactory)this.set).getName());

        }
        System.out.println("done");
    }
}
