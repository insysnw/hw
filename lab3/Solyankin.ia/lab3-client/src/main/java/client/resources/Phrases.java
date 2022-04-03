package client.resources;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum Phrases {

    USER_CONNECTED("Welcome to the calculator client\n" +
            "To get started, enter the expression you want to calculate and press \"enter\"\n" +
            "For details type \"/info\" \n"),
    USER_DISCONNECTED("Goodbye!\n" +
            "We hope we helped you find answers to the necessary expressions\n"),
    USER_INFO("You can enter an expression in the format: a(+-*/)b\n" +
            "Or use complex operators: fact(a) or sqrt(a)\n" +
            "To close client enter \"quit\""),

    CLIENT_INCORRECT_EXPRESSION_ERROR("Incorrect expression. Check your input\n"),
    CLIENT_INPUT_ERROR("Error while reading from keyboard. Check your expression and try again\n"),

    CLIENT_COMMAND_QUIT("/quit"),
    CLIENT_COMMAND_INFO("/info"),
    SERVER_COMMAND_STOP_SERVER("/stop"),

    FACT_COMMAND("fact"),
    SQRT_COMMAND("sqrt");

    private String phrase;

    public static Phrases fromString(String text) {
        for (Phrases phrase : Phrases.values()) {
            if (phrase.phrase.equalsIgnoreCase(text)) {
                return phrase;
            }
        }
        return CLIENT_INCORRECT_EXPRESSION_ERROR;
    }
}
