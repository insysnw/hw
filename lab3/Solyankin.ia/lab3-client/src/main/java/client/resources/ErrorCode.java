package client.resources;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ErrorCode {
    MISSED_OPERAND("Missed operand. Check your expression\n"),
    EXTRA_OPERAND("Extra operand. Check your expression\n"),
    WRONG_OPERAND_FORMAT("Wrong operand command. Check your expression\n"),
    DIVISION_BY_ZERO("ALARM!!!! DIVISION BY ZERO ON THE APPROACH!!!\n"),
    SQRT_OF_NEGATIVE("You try get SQRT from negative variable. Check your expression\n"),
    FACTORIAL_OF_NEGATIVE_OR_FLOAT("You try get FACT from negative or float variable. Check your expression\n"),
    UNKNOWN_OPERATOR("Unknown operator. Check your expression\n"),
    UNKNOWN_ERROR_CODE("Unknown error code\n");

    private String errorMessage;
}
