
public class TestVersionsBST {

    public static void main(String[] args) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        ValueCell vc = new ValueCell(26);
        vc.putNewValue(1);
        vc.casLatestVersion(0,2);
        vc.putNewValue(5);
        var casRes = vc.casLatestVersion(0,6);
        vc.putNewValue(58);
        // var casRes2 = vc.casLatestVersion(0,7);

        var latestValue = vc.getLatestValue();
        var valueByVersion5 = vc.helpAndGetValueByVersion(5);
        var valueByVersion888 = vc.helpAndGetValueByVersion(799);
    }
}
