package client.http;

import client.model.CalculatorRequest;
import client.model.CalculatorResponseBad;
import client.model.CalculatorResponseOk;
import client.resources.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpRequest.newBuilder;

public class HttpHandler {
    private String host;
    private HttpClient client;

    public HttpHandler(String host) {
        this.host = host;
        this.client = HttpClient.newBuilder().build();
    }

    public void httpPostRequest(CalculatorRequest calculatorRequest) {
        try {
            HttpRequest request = newBuilder(new URI(host + "/restapi/calculate"))
                    .POST(HttpRequest.BodyPublishers.ofString(getJson(calculatorRequest)))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            responseHandling(response);
        } catch (URISyntaxException | InterruptedException | IOException e) {
            System.out.println("Error in PostRequest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void responseHandling(HttpResponse<String> response) {
        ObjectMapper objectMapper = new ObjectMapper();
        String body = response.body();
        int code = response.statusCode();
        if (code / 100 == 2) {
            try {
                CalculatorResponseOk responseCalc = objectMapper.readValue(body, CalculatorResponseOk.class);
                System.out.println("Result is: " + responseCalc.getResult() + "\n");
            } catch (IOException e) {
                System.out.println("Error while reading Json: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            badRequestHandling(objectMapper, body);
        }
    }

    private void badRequestHandling(ObjectMapper objectMapper, String body) {
        try {
            CalculatorResponseBad responseCalc = objectMapper.readValue(body, CalculatorResponseBad.class);
            switch (responseCalc.getErrorCode().trim()) {
                case "DIVISION_BY_ZERO":
                    System.out.println(ErrorCode.DIVISION_BY_ZERO.getErrorMessage());
                    break;
                case "EXTRA_OPERAND":
                    System.out.println(ErrorCode.EXTRA_OPERAND.getErrorMessage());
                    break;
                case "MISSED_OPERAND":
                    System.out.println(ErrorCode.MISSED_OPERAND.getErrorMessage());
                    break;
                case "SQRT_OF_NEGATIVE":
                    System.out.println(ErrorCode.SQRT_OF_NEGATIVE.getErrorMessage());
                    break;
                case "UNKNOWN_OPERATOR":
                    System.out.println(ErrorCode.UNKNOWN_OPERATOR.getErrorMessage());
                    break;
                case "UNKNOWN_ERROR_CODE":
                    System.out.println(ErrorCode.UNKNOWN_ERROR_CODE.getErrorMessage());
                    break;
                case "WRONG_OPERAND_FORMAT":
                    System.out.println(ErrorCode.WRONG_OPERAND_FORMAT.getErrorMessage());
                    break;
                case "FACTORIAL_OF_NEGATIVE_OR_FLOAT":
                    System.out.println(ErrorCode.FACTORIAL_OF_NEGATIVE_OR_FLOAT.getErrorMessage());
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error while reading Json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getJson(CalculatorRequest calculatorRequest) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String partsInString = objectMapper.writeValueAsString(calculatorRequest);
        JsonNode jsonParts = objectMapper.readTree(partsInString);
        return jsonParts.toString();
    }
}
