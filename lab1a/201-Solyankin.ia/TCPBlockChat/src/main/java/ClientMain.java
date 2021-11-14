import client.Client;

public class ClientMain {
    private static Integer port = 8888;
    private static String host = "localhost";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("HOST and PORT are not provided, using default");
        } else {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }

        System.out.println("Starting client... " + host + ":" + port);
        Client client = new Client(host, port);
        client.start();
    }
}
