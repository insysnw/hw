package protocol.messages;

public class MessageFromServer {
    private String id;
    private ResultWithErrCode resultWithErrCode;


    public MessageFromServer(String id, ResultWithErrCode resultWithErrCode) {
        this.id = id;
        this.resultWithErrCode = resultWithErrCode;
    }

    public MessageFromServer() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ResultWithErrCode getResultWithErrCode() {
        return resultWithErrCode;
    }

    public void setResultWithErrCode(ResultWithErrCode resultWithErrCode) {
        this.resultWithErrCode = resultWithErrCode;
    }

    @Override
    public String toString() {
        return "Message: id - " + id +
                " result - " + resultWithErrCode.getResult() +
                " errCode - " + resultWithErrCode.getErrCode();
    }

    public String toMessage() {
        return id + resultWithErrCode.toString();
    }
}
