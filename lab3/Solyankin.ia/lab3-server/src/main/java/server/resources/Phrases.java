package server.resources;

public enum Phrases {
    DEFAULT_HOST_AND_PORT("HOST and PORT are not provided, using default"),
    USER_DISCONNECTED(" disconnected"),
    USER_CONNECTED(" connected"),

    SERVER_STARTING("Starting server on "),
    SERVER_WELCOME("Calculator server is listening on port "),
    SERVER_STOPPED("Server stopped"),

    SERVER_WELCOME_ERROR("Error while creating ServerSocket: "),
    SERVER_INCORRECT_COMMAND_ERROR("Incorrect command"),
    SERVER_CLOSE_USERTHREAD_ERROR("Error while closing userThread: "),

    CLIENT_COMMAND_QUIT("/quit"),
    SERVER_COMMAND_STOP_SERVER("/stop");

    private String phrase;

    Phrases(String phrase) {
        this.phrase = phrase;
    }

    public String getPhrase() {
        return phrase;
    }

    public static Phrases fromString(String text) {
        for (Phrases phrase : Phrases.values()) {
            if (phrase.phrase.equalsIgnoreCase(text)) {
                return phrase;
            }
        }
        return SERVER_INCORRECT_COMMAND_ERROR;
    }
}
