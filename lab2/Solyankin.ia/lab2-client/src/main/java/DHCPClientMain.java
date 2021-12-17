import client.DHCPClient;

public class DHCPClientMain {
    public static void main(String[] args) {
        System.out.println("Starting client");
        DHCPClient client = new DHCPClient();
        client.start();
    }
}
