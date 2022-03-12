package protocol.messages;

public class MessageFromClient {
    private String id;
    private String opcode;
    private String arg1;
    private String arg2;

    public MessageFromClient(String id, String opcode, String arg1, String arg2) {
        this.id = id;
        this.opcode = opcode;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public MessageFromClient() {
    }

    public String getId() {
        return id;
    }

    public Integer getArg1() {
        return Integer.parseInt(arg1);
    }

    public Integer getArg2() {
        return Integer.parseInt(arg2);
    }

    public String getOpcode() {
        return opcode;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setArg1(String arg1) {
        this.arg1 = arg1;
    }

    public void setArg2(String arg2) {
        this.arg2 = arg2;
    }

    public void setOpcode(String opcode) {
        this.opcode = opcode;
    }
}
