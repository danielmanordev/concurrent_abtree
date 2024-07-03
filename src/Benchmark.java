import benchmark.JavaConcurrentSkipList;
import benchmark.occabtreewithscan.src.OCCABTreeWithScanSet;

public class Benchmark {
    public static void main(String[] args) {
        int numberOfThreads = 80;

        System.out.println("WARMUP - START");
        var wu = new WarmupBenchmark(new MTASet(2,256),80,20,5,10);
        wu.run();
        System.out.println("WARMUP - END");



    //    System.out.println("SCAN ONLY - START");
    //    System.out.println("JavaConcurrentSkipList");
    //    var jcs = new ScanOnlyBenchmark(new JavaConcurrentSkipList(),numberOfThreads);
    //    jcs.run();
    //    System.out.println("OCCABTreeWithScanSet");
    //    var occAabws = new ScanOnlyBenchmark(new OCCABTreeWithScanSet(2,256),numberOfThreads);
    //    occAabws.run();
    //    System.out.println("MTASet");
    //    var mtaso = new ScanOnlyBenchmark(new MTASet(2,256),numberOfThreads);
    //    mtaso.run();
    //    System.out.println("SCAN ONLY - END");
    //    ////////////////////////////////////////////////////////
//
    //    System.out.println("80% Insert 20% Delete - START");
    //    System.out.println("JavaConcurrentSkipList");
    //    var jcs8020 = new InsertDeleteGetBenchmark(new JavaConcurrentSkipList(),80,20,0,numberOfThreads);
    //    jcs8020.run();
    //    System.out.println("OCCABTreeWithScanSet");
    //    var occab8020 = new InsertDeleteGetBenchmark(new OCCABTreeWithScanSet(2,256),80,20,0,numberOfThreads);
    //    occab8020.run();
//
    //    System.out.println("MTASet");
    //    var mta8020 = new InsertDeleteGetBenchmark(new MTASet(2,256),80,20,0,numberOfThreads);
    //    mta8020.run();
    //    System.out.println("80% Insert 20% Delete - END");

        ////////////////////////////////////////////////////////

      //  qqSystem.out.println("Scan 32k, 80% Insert 20% Delete - START");

     System.out.println("OCCABTreeWithScanSet");
      var occabScan8020 = new PutOnlyBenchmark(new OCCABTreeWithScanSet(2,256),7);
       occabScan8020.run();
       System.out.println("MTASet");
       var mtaSetScan3280insert20Delete = new PutOnlyBenchmark(new MTASet(2,256),7);
       mtaSetScan3280insert20Delete.run();
        System.out.println("JavaConcurrentSkipList");
        var jcsScan3280insert20Delete = new PutOnlyBenchmark(new JavaConcurrentSkipList(),7);
         jcsScan3280insert20Delete.run();
        System.out.println("PO - END");

        System.out.println("OCCABTreeWithScanSet");
        var occabScan80203 = new InsertDeleteGetBenchmark(new OCCABTreeWithScanSet(2,256),0,0,100,7);
        occabScan80203.run();
        System.out.println("MTASet");
        var mtaSetScan3280insert320Delete = new InsertDeleteGetBenchmark(new MTASet(2,256),0,0,100,7);
        mtaSetScan3280insert320Delete.run();
        System.out.println("JavaConcurrentSkipList");
        var jcsScan3280insert20D3elete = new InsertDeleteGetBenchmark(new JavaConcurrentSkipList(),0,0,100,7);
        jcsScan3280insert20D3elete.run();
        System.out.println("GO- END");


      /*  System.out.println("MTASet");
        var mtaSet = new InsertDeleteGetBenchmark(new MTASet(2,256),80,20,0,numberOfThreads);
        mtaSet.run();

        System.out.println("JavaConcurrentSkipList");
        var jcs = new InsertDeleteGetBenchmark(new JavaConcurrentSkipList(),80,20,0,numberOfThreads);
        jcs.run();
*/
    }
}
