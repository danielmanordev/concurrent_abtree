import benchmark.JavaConcurrentSkipList;

public class Benchmark {
    public static void main(String[] args) {
        System.out.println("SCAN ONLY");
        System.out.println("JavaConcurrentSkipList");
        var jcs = new ScanOnlyBenchmark(new JavaConcurrentSkipList());
        jcs.run();
        System.out.println("MTASet");
        var mtaso = new ScanOnlyBenchmark(new MTASet(2,256));
        mtaso.run();

        System.out.println("80% Insert 20% Delete");
        System.out.println("JavaConcurrentSkipList");
        var jcs8020 = new Put80Delete20Benchmark(new JavaConcurrentSkipList());
        jcs8020.run();
        System.out.println("MTASet");
        var mta8020 = new Put80Delete20Benchmark(new MTASet(2,256));
        mta8020.run();



    }
}
