package protocol.messages;

public class ResultWithErrCode {
    private String result;
    private String errCode;

    public ResultWithErrCode(String result, String errCode) {
        this.errCode = errCode;
        this.result = result;
    }

    public ResultWithErrCode() {
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    String getResult() {
        return result;
    }

    String getErrCode() {
        return errCode;
    }

    @Override
    public String toString() {
        return result + errCode;
    }
}
