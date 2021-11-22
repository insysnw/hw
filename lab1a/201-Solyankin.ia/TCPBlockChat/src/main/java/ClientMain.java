import client.Client;
import resources.Phrases;

public class ClientMain {
    private static Integer port = 8888;
    private static String host = "0.0.0.0";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println(Phrases.DEFAULT_HOST_AND_PORT.getPhrase());
        } else {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }

        System.out.println(Phrases.CLIENT_STARTING.getPhrase() + host + ":" + port);
        Client client = new Client(host, port);
        client.start();
    }
}
