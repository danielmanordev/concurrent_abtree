import abstractions.Set;
import abstractions.SetFactory;
import benchmark.Test;

import java.util.ArrayList;

public class PutOnlyBenchmark implements Test {
    private Set set;
    private int numberOfTests;
    public PutOnlyBenchmark(Set set, int numberOfTests){
        this.set = set;
        this.numberOfTests = numberOfTests;
    }
    @Override
    public void run() {

        int availableProcessors = Runtime.getRuntime().availableProcessors();

        int dataRange = 1000000;
        int numberOfThreads = 1;
        int testDuration=10000;



        System.out.println("Starting, "+ 100+"% insert, "+0+"% delete "+0+"% get");
        for (int i = 0; i < this.numberOfTests; i++) {
            double ratePerSec =0.0;
            for (int j=0;j<10;j++){
                TestSet.seed(set,dataRange,dataRange/2);
                TestResult testResult = TestSet.runTest(set, numberOfThreads, 0 ,dataRange, 0, 100,1,32000,testDuration,false);
                double perSec = (testResult.TotalAdds.longValue()/10);
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
