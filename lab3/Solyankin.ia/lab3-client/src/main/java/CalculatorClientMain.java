import client.CalculatorClient;

public class CalculatorClientMain {
    private static final String HOST = "http://localhost:1000/";

    public static void main(String[] args) {
        CalculatorClient client = new CalculatorClient(HOST);
        client.start();
    }
}
