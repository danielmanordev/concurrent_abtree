// the entry-point for the testbed is the static runTest() method

import abstractions.Set;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

class TestSet extends Thread
{
    private final Set set;
    private  int	dataRange;
    private  int	perCon;
    private  int	perAdd;
    private  int	low;
    private  int	high;
    private  boolean	scanOnly = false;
    private BigInteger numberOfAdds = BigInteger.ZERO;
    private BigInteger numberOfContains = BigInteger.ZERO;
    private BigInteger numberOfRemoves = BigInteger.ZERO;
    private BigInteger numberOfScannedKeys = BigInteger.ZERO;
    private List<String> prints;
    public TestSet(Set set, int dataRange, int perCon, int perAdd)
    {
        this.set = set;
        this.dataRange = dataRange;
        this.perCon = perCon;
        this.perAdd = perAdd;
        this.prints = new ArrayList<>();
    }

    public TestSet(Set set, int low, int high)
    {
        this.set = set;
        this.low = low;
        this.high = high;
        this.scanOnly =true;
        this.prints = new ArrayList<>();
    }

    public void run()
    {
        try {
            while (true) {

              
                 if(this.scanOnly) {
                    /*if(set.remove(randomInt)){
                        //prints.add(randomInt +  " was removed");
                    }
                    else {
                        //prints.add(randomInt +  " was NOT removed");
                    }*/
                     int scannedKeys = set.getRange(new int[this.high-this.low+1],this.low,this.high);
                     numberOfScannedKeys=numberOfScannedKeys.add(BigInteger.valueOf(scannedKeys));

                } else {

                     var randomInt = ThreadLocalRandom.current().nextInt(1,this.dataRange);

                     var opValue = ThreadLocalRandom.current().nextInt(0,100); // uniformly distributed


                     if(opValue >= 0 && opValue < this.perCon) {

                   /* if(set.contains(randomInt)){
                        prints.add(randomInt + " exists");
                    }
                    else {
                        prints.add(randomInt + " not found");
                    }*/
                         set.contains(randomInt);
                         numberOfContains=numberOfContains.add(BigInteger.ONE);
                     }

                     // add
                     else if (opValue >= this.perCon && opValue < this.perAdd+this.perCon){
                   /*if(){

                    }
                    else {
                        prints.add(randomInt + " exists and was not added") ;
                    }*/
                         set.add(randomInt, randomInt);
                         numberOfAdds=numberOfAdds.add(BigInteger.ONE);
                     }
                     else {
                   /*if(){

                    }
                    else {
                        prints.add(randomInt + " exists and was not added") ;
                    }*/
                         set.remove(randomInt);
                         numberOfRemoves=numberOfRemoves.add(BigInteger.ONE);
                     }
                     
                 }

                 
                if (Thread.interrupted()) {// Clears interrupted status!}
                   // System.out.println("Thread Id: "+Thread.currentThread().getId()+"   interrupted");
                    throw new InterruptedException();
                }
            }
        }
        catch (InterruptedException iex) {

        }
    }

    public static void seed(Set set, int dataRange, int numberOfKeys){

        for (int i=0;i<numberOfKeys;i++){
            int key = ThreadLocalRandom.current().nextInt(1,dataRange);
            int value = ThreadLocalRandom.current().nextInt(1,dataRange);
            set.add(key,value);
        }
    }

    /** This is the base method for the TestSet class, which executes a single test.
     * @param set the set to test
     * @param numThreads number of threads for the current set
     * @param dataRange range of datas to test [0,dataRange)
     * @param perCon percentage of contains(x) operations
     * @param perAdd percentage of add(x) operations
     * @param ms length of test (in milliseconds)
     */
    public static TestResult runTest(Set set, int numThreads, int numberOfScanThreads ,int dataRange, int perCon, int perAdd, int scanLow, int scanHigh ,int ms)
    {
        int numberOfNonScanThreads = numThreads - numberOfScanThreads;
        

        // create non scan threads for the test
        TestSet[] threads = new TestSet[numThreads];
        
        for (int i=0;i < numberOfNonScanThreads; ++i)
            threads[i] = new TestSet(set, dataRange, perCon, perAdd);

        // create scan threads for the test
        for (int i = numberOfNonScanThreads; i < numThreads; ++i)
            threads[i] = new TestSet(set, scanLow, scanHigh);

        // start threads
        for (int i = 0; i < numThreads; ++i) {
            threads[i].start();
        }

        // wait
        try {
            Thread.sleep(ms);
        } catch (InterruptedException iex) {}

        // stop threads
        for (int i = 0; i < numThreads; ++i) {
            threads[i].interrupt();
        }
         TestResult testResult = new TestResult();
        // print details
        for (int i = 0; i < numThreads; ++i) {
            for (int j=0; j< threads[i].prints.size();++j){
                System.out.println("Thread "+ i +" "+threads[i].prints.get(j));
            }
            testResult.TotalRemoves = testResult.TotalRemoves.add(threads[i].numberOfRemoves);
            testResult.TotalAdds = testResult.TotalAdds.add(threads[i].numberOfAdds);
            testResult.TotalContains = testResult.TotalContains.add(threads[i].numberOfContains);
            testResult.numberOfScannedKeys = testResult.numberOfScannedKeys.add(threads[i].numberOfScannedKeys);
        }

        //System.out.println("Total add invocations: "+ testResult.TotalAdds );
        //System.out.println("Total remove invocations: "+ testResult.TotalRemoves );
        //System.out.println("Total contains invocations: "+ testResult.TotalContains );
        testResult.Total = testResult.Total.add(testResult.TotalAdds);
        testResult.Total = testResult.Total.add(testResult.TotalRemoves);
        testResult.Total = testResult.Total.add(testResult.TotalContains);
        testResult.Total = testResult.Total.add(testResult.numberOfScannedKeys);
        //System.out.println("Total invocations: "+ testResult.Total );
        return testResult;

    }
}