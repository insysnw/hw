package resources;

public enum Phrases {
    //Common phrases
    DEFAULT_HOST_AND_PORT("HOST and PORT are not provided, using default"),
    USER_QUITED(" quited"),
    USER_JOINED(" joined"),
    SEND_MESSAGE_ERROR("Error while send message: "),
    SEND_FILE("Sent the file: "),

    //Server phrases
    SERVER_STARTING("Starting server on "),
    SERVER_WELCOME("Chat server is listening on port "),
    SERVER_STOPPED("Server stopped"),

    //Server error phrases
    SERVER_WELCOME_ERROR("Error while creating ServerSocket: "),
    SERVER_CONNECT_ERROR("Server Connect failed, name already exist"),
    SERVER_SEND_BYTES_ERROR("Error while send bytes: "),
    SERVER_CLOSE_USERTHREAD_ERROR("Error while closing userThread: "),
    SERVER_INCORRECT_COMMAND_ERROR("Incorrect command"),

    //Client phrases
    CLIENT_STARTING("Starting client...\nAttempting to connect to the server: "),
    CLIENT_WELCOME("Connected to the chat server"),
    CLIENT_ENTER_NAME("Enter your name: "),
    CLIENT_FILE_SENDING("Server File sending( "),
    CLIENT_SERVER_CLOSED("Server closed"),
    CLIENT_CLOSED("Client closed"),
    CLIENT_FILE_SAVED("Client File saved: "),
    CLIENT_FILE_OVERWRITING("Client File overwriting: "),
    CLIENT_DIRECTORY_CREATED("Client Directory created: "),

    //Client error phrases
    CLIENT_WELCOME_ERROR("Server not found"),
    CLIENT_ENTER_NAME_ERROR("Error while entering name: server closed"),
    CLIENT_INCORRECT_NAME_ERROR("Client Incorrect UserName. You can use only letters(A-z) and numbers(0-9)"),
    CLIENT_INVALID_FILE_ERROR("Invalid file"),
    CLIENT_INVALID_FILE_SIZE("File size exceeded (5MB)"),
    CLIENT_CLOSE_SENDMESSAGETHREAD_ERROR("Error closing SendMessageThread: "),

    //Command phrases
    SERVER_COMMAND_STOP_SERVER("/stop"),
    SERVER_COMMAND_SHOW_USERS("/users"),
    CLIENT_COMMAND_QUIT("/quit"),
    CLIENT_COMMAND_FILE("/file");

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
