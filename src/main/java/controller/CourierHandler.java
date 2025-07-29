package controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.ErrorDto;
import service.CourierService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CourierHandler implements HttpHandler {
    private final CourierService courierService;
    private final Gson gson;

    public CourierHandler() {
        this.courierService = new CourierService();
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/deliveries/available") && method.equals("GET")) {
                courierService.getAvailableDeliveries(exchange);
            } else if (path.matches("/deliveries/\\d+") && method.equals("PATCH")) {
                Long orderId = Long.parseLong(path.split("/")[2]);
                Map<String, String> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                courierService.updateDeliveryStatus(exchange, orderId, requestBody);
            } else if (path.equals("/deliveries/history") && method.equals("GET")) {
                courierService.getDeliveryHistory(exchange);
            } else {
                courierService.sendResponse(exchange, new ErrorDto("Not found"), 404);
            }
        } catch (Exception e) {
            courierService.sendResponse(exchange    , new ErrorDto("Internal server error: " + e.getMessage()), 500);
        }
    }
}