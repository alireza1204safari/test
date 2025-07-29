package handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.ErrorDto;
import service.TransactionService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TransactionHandler implements HttpHandler {
    private final TransactionService transactionService;
    private final Gson gson;

    public TransactionHandler() {
        this.transactionService = new TransactionService();
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/transactions") && method.equals("GET")) {
                transactionService.getTransactions(exchange);
            } else if (path.equals("/wallet/top-up") && method.equals("POST")) {
                Map<String, Object> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                transactionService.topUpWallet(exchange, requestBody);
            } else if (path.equals("/payment/online") && method.equals("POST")) {
                Map<String, Object> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                transactionService.onlinePayment(exchange, requestBody);
            } else {
                transactionService.sendResponse(exchange, new ErrorDto("Not found"), 404);
            }
        } catch (Exception e) {
            transactionService.sendResponse(exchange, new ErrorDto("Internal server error: " + e.getMessage()), 500);
        }
    }
}