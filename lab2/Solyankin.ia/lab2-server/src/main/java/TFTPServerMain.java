import server.TFTPServer;

public class TFTPServerMain {
    private static Integer port = 69;
    private static String host = "localhost";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("HOST and PORT are not provided, using default");
        } else {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }

        System.out.println("Starting server on " + host + ":" + port);
        TFTPServer server = new TFTPServer(host, port);
        server.start();
    }
}
