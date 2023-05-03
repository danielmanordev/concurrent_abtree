// the entry-point for the testbed is the static runTest() method

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

class TestSet extends Thread
{
    private final Set set;
    private final int	dataRange;
    private final int	perCon;
    private final int	perAdd;
    private int numberOfAdds=0;
    private int numberOfContains=0;
    private int numberOfRemoves=0;
    private List<String> prints;
    public TestSet(Set set, int dataRange, int perCon, int perAdd)
    {
        this.set = set;
        this.dataRange = dataRange;
        this.perCon = perCon;
        this.perAdd = perAdd;
        this.prints = new ArrayList<>();
    }

    public void run()
    {
        try {
            while (true) {

                var randomInt = ThreadLocalRandom.current().nextInt(1,this.dataRange);

                var opValue = ThreadLocalRandom.current().nextInt(0,100); // uniformly distributed

                // contains
                if(opValue >= 0 && opValue < this.perCon) {

                    if(set.contains(randomInt)){
                        prints.add(randomInt + " exists");
                    }
                    else {
                        prints.add(randomInt + " not found");
                    }
                    numberOfContains++;
                }

                // add
                else if (opValue >= this.perCon && opValue <this.perCon+this.perAdd){
                    if(set.add(randomInt, randomInt)){
                        //prints.add(randomInt + " was added");
                    }
                    else {
                        prints.add(randomInt + " exists and was not added") ;
                    }
                    numberOfAdds++;
                }
                // remove
                else {
                    if(set.remove(randomInt)){
                        prints.add(randomInt +  " was removed");
                    }
                    else {
                        prints.add(randomInt +  " was NOT removed");
                    }
                    numberOfRemoves++;
                }

                if (Thread.interrupted()) {// Clears interrupted status!}
                    System.out.println("Thread Id: "+Thread.currentThread().getId()+"   interrupted");
                    throw new InterruptedException();
                }
            }
        }
        catch (InterruptedException iex) {

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
    public static void runTest(Set set, int numThreads, int dataRange, int perCon, int perAdd, int ms)
    {
        int totalAdds=0;
        int totalRemoves=0;
        int totalContains=0;

        // create all threads for the test
        TestSet[] threads = new TestSet[numThreads];
        for (int i = 0; i < numThreads; ++i)
            threads[i] = new TestSet(set, dataRange, perCon, perAdd);

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

        // print details
        for (int i = 0; i < numThreads; ++i) {
            for (int j=0; j< threads[i].prints.size();++j){
                System.out.println("Thread "+ i +" "+threads[i].prints.get(j));
            }
            totalRemoves  += threads[i].numberOfRemoves;
            totalAdds     += threads[i].numberOfAdds;
            totalContains += threads[i].numberOfContains;
        }

        System.out.println("Total add invocations: "+ totalAdds );
        System.out.println("Total remove invocations: "+ totalRemoves );
        System.out.println("Total contains invocations: "+ totalContains );
        int total = totalContains+totalAdds+totalRemoves;
        System.out.println("Total invocations: "+ total );
    }
}