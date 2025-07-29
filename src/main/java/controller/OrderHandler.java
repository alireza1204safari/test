package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.OrderService;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderHandler implements HttpHandler {
    private final OrderService orderService;

    public OrderHandler() {
        this.orderService = new OrderService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // POST /orders
        if (path.equals("/orders") && "POST".equals(method)) {
            orderService.handleSubmitOrder(exchange);
            return;
        }

        // GET /orders/{id}
        Pattern orderPattern = Pattern.compile("^/orders/(\\d+)$");
        Matcher orderMatcher = orderPattern.matcher(path);
        if (orderMatcher.matches() && "GET".equals(method)) {
            Long orderId = Long.parseLong(orderMatcher.group(1));
            orderService.handleGetOrderDetails(exchange, orderId);
            return;
        }

        // GET /orders/history
        if (path.equals("/orders/history") && "GET".equals(method)) {
            orderService.handleGetOrderHistory(exchange);
            return;
        }

        // Handle invalid path or method
        if ("POST".equals(method) || "GET".equals(method)) {
            exchange.sendResponseHeaders(404, -1); // Not Found
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }
}