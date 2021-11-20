import server.Server;

public class ServerMain {
    private static Integer port = 8888;
    private static String host = "0.0.0.0";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("HOST and PORT are not provided, using default");
        } else {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }

        System.out.println("Starting server on " + host + ":" + port);
        Server server = new Server(host, port);
        server.start();
    }
}
