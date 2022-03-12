package server.calculator;

import org.apache.commons.math3.util.CombinatoricsUtils;
import protocol.messages.ResultWithErrCode;

public class Calculator {
    public static final String SUM = "0";
    public static final String SUB = "1";
    public static final String MUL = "2";
    public static final String DIV = "3";
    public static final String FACT = "4";
    public static final String SQRT = "5";

    public Calculator() {
    }

    public ResultWithErrCode sum(Integer arg1, Integer arg2) {
        return new ResultWithErrCode(String.valueOf(arg1 + arg2), "0");
    }

    public ResultWithErrCode sub(Integer arg1, Integer arg2) {
        return new ResultWithErrCode(String.valueOf(arg1 - arg2), "0");
    }

    public ResultWithErrCode mul(Integer arg1, Integer arg2) {
        return new ResultWithErrCode(String.valueOf(arg1 * arg2), "0");
    }

    public ResultWithErrCode div(Integer arg1, Integer arg2) {
        int result = 0;
        int errCode = 0;
        try {
            result = arg1 / arg2;
        } catch (NumberFormatException e) {
            errCode = 1;
        }
        return new ResultWithErrCode(Integer.toString(result), Integer.toString(errCode));
    }

    public ResultWithErrCode frac(Integer arg1) {
        long result = 0;
        int errCode = 0;
        if (arg1 >= 0) {
            result = CombinatoricsUtils.factorial(arg1);
        } else {
            errCode = 3;
        }
        return new ResultWithErrCode(Long.toString(result), Integer.toString(errCode));
    }

    public ResultWithErrCode sqrt(Integer arg1) {
        double result = 0;
        int errCode = 0;
        if (arg1 >= 0) {
            result = Math.sqrt(arg1);
        } else {
            errCode = 3;
        }
        return new ResultWithErrCode(Double.toString(result), Integer.toString(errCode));
    }
}
