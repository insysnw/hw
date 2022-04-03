package client.resources;

import java.math.BigDecimal;
import java.util.ArrayList;

public class Parts {
    private ArrayList<String> operands;
    private String operator;

    public Parts(ArrayList<String> operands, String operator) {
        this.operands = operands;
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public ArrayList<String> getOperands() {
        return operands;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setOperands(ArrayList<String> operands) {
        this.operands = operands;
    }

    public void addOperands(String operand) {
        operands.add(operand);
    }
}
