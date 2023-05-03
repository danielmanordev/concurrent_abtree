public class Result {

    private int value;
    private ReturnCode returnCode;


    public Result(int value, ReturnCode returnCode){
        this.value = value;
        this.returnCode = returnCode;
    }

    public Result(ReturnCode returnCode){
        this.returnCode = returnCode;
    }


    public int getValue() {
        return this.value;
    }

    public ReturnCode getReturnCode(){
        return this.returnCode;
    }
}
