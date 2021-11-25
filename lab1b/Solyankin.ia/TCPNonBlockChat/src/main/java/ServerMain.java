import resources.Phrases;
import server.Server;

import java.io.IOException;

public class ServerMain {
    private static Integer port = 8888;
    private static String host = "0.0.0.0";

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println(Phrases.DEFAULT_HOST_AND_PORT.getPhrase());
        } else {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }

        System.out.println(Phrases.SERVER_STARTING.getPhrase() + host + ":" + port);
        Server server = new Server(host, port);
        server.start();
    }
}
