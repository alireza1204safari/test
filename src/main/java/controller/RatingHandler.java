package controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.ErrorDto;
import service.RatingService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RatingHandler implements HttpHandler {
    private final RatingService ratingService;
    private final Gson gson;

    public RatingHandler() {
        this.ratingService = new RatingService();
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/ratings") && method.equals("POST")) {
                Map<String, Object> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                ratingService.addRating(exchange, requestBody);
            } else if (path.matches("/ratings/items/\\d+") && method.equals("GET")) {
                Long itemId = Long.parseLong(path.split("/")[3]);
                ratingService.getRatingsByItemId(exchange, itemId);
            } else if (path.matches("/ratings/\\d+") && method.equals("GET")) {
                ratingService.getRatings(exchange);
            } else if (path.matches("/ratings/\\d+") && method.equals("PUT")) {
                Long ratingId = Long.parseLong(path.split("/")[2]);
                Map<String, Object> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                ratingService.updateRating(exchange, ratingId, requestBody);
            } else if (path.matches("/ratings/\\d+") && method.equals("DELETE")) {
                Long ratingId = Long.parseLong(path.split("/")[2]);
                ratingService.deleteRating(exchange, ratingId);
            } else {
                sendResponse(exchange, new ErrorDto("Not found"), 404);
            }
        } catch (Exception e) {
            sendResponse(exchange, new ErrorDto("Internal server error: " + e.getMessage()), 500);
        }
    }
    private void sendResponse(HttpExchange exchange, Object response, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        String jsonResponse = gson.toJson(response);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}