package client.http;

import client.resources.Parts;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

    public void httpPostRequest(Parts parts) {
        try {
            HttpRequest request = newBuilder(new URI(host + "/"))
                    .POST(HttpRequest.BodyPublishers.ofString(getJson(parts)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            System.out.println("httpPostRequest : " + responseBody);
        } catch (URISyntaxException | InterruptedException | IOException e) {
            System.out.println("Error in PostRequest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getJson(Parts parts) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        gson.toJson(parts);
        System.out.println(gson.toJson(parts));
        return gson.toJson(parts);
    }
}
