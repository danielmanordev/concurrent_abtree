import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

       /* Set set = new OCCABTree(2,Constants.DEGREE);
        set.add(1,1);
        set.add(2,1);
        set.add(3,1);
        set.add(4,1);
        set.add(5,1);
        set.add(6,1);
        set.add(7,1);
        set.add(8,1);
        set.add(9,1);
        set.add(10,1);
        set.add(11,1);
        set.add(12,1);
        set.add(13,1);
        set.add(14,1);
        set.add(15,1);
        set.add(16,1);
        set.add(17,1);
        set.add(18,1);

        boolean res = set.contains(19);
        boolean res2 = set.contains(2);
      */

       for (int i = 1; i <= 1; i++) {
            int numberOfThreads =3;//nteger.parseInt(args[1]);
            Set concurrenSet = new OCCABTree(2, Constants.DEGREE);


            long start = System.currentTimeMillis();
            TestSet.runTest(concurrenSet, numberOfThreads, 1000, 0, 100, 10000);
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;

            System.out.println("Total time: " + timeElapsed + " ms with " + numberOfThreads + " threads");

        }
    }
     }
