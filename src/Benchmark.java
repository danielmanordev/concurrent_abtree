import benchmark.JavaConcurrentSkipList;
import benchmark.occabtreewithscan.src.OCCABTreeWithScanSet;

public class Benchmark {
    public static void main(String[] args) {
        System.out.println("SCAN ONLY");
        System.out.println("OCCABTreeWithScanSet");
        var occAabws = new ScanOnlyBenchmark(new OCCABTreeWithScanSet(2,256),8);
        occAabws.run();
        System.out.println("JavaConcurrentSkipList");
        var jcs = new ScanOnlyBenchmark(new JavaConcurrentSkipList(),8);
        jcs.run();
        System.out.println("MTASet");
        var mtaso = new ScanOnlyBenchmark(new MTASet(2,256),8);
        mtaso.run();

        System.out.println("80% Insert 20% Delete - START");
        System.out.println("JavaConcurrentSkipList");
        var jcs8020 = new InsertDeleteGetBenchmark(new JavaConcurrentSkipList(),80,20,0,8);
        jcs8020.run();
        System.out.println("MTASet");
        var mta8020 = new InsertDeleteGetBenchmark(new MTASet(2,256),80,20,0,8);
        mta8020.run();
        System.out.println("80% Insert 20% Delete - END");
        System.out.println("Scan 32k, 80% Insert 20% Delete - START");

        System.out.println("JavaConcurrentSkipList");
        var jcsScan3280insert20Delete = new Scan32KWithInsertsAndDeletesBenchmark(new JavaConcurrentSkipList(),80,20,9);
        jcsScan3280insert20Delete.run();
        System.out.println("MTASet");
        var mtaSetScan3280insert20Delete = new Scan32KWithInsertsAndDeletesBenchmark(new MTASet(2,256),80,20,9);
        mtaSetScan3280insert20Delete.run();
        System.out.println("Scan 32k, 80% Insert 20% Delete - END");




    }
}
