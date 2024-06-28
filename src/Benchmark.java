import benchmark.JavaConcurrentSkipList;

public class Benchmark {
    public static void main(String[] args) {
        System.out.println("SCAN ONLY");
        System.out.println("JavaConcurrentSkipList");
        //   run(new JavaConcurrentSkipList());
        System.out.println("MTASet");
        var so = new ScanOnlyBenchmark(new MTASet(2,256));
        so.run();
    }
}
