import client.DHCPClient;

public class DHCPClientMain {
    private static Integer port = 68;
    private static String host = "192.168.0.5";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("HOST and PORT are not provided, using default");
        } else {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }

        System.out.println("Starting client");
        DHCPClient client = new DHCPClient(host, port);
        client.start();
    }
}
