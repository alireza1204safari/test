package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.ItemService;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemHandler implements HttpHandler {

    private final ItemService itemService;

    public ItemHandler() {
        this.itemService = new ItemService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // POST /items
        if (path.equals("/items") && "POST".equals(method)) {
            itemService.handleListItems(exchange);
            return;
        }

        // GET /items/{id}
        Pattern itemPattern = Pattern.compile("^/items/(\\d+)$");
        Matcher itemMatcher = itemPattern.matcher(path);
        if (itemMatcher.matches() && "GET".equals(method)) {
            Long itemId = Long.parseLong(itemMatcher.group(1));
            itemService.handleGetItemDetails(exchange, itemId);
            return;
        }

        // Handle invalid path or method
        if ("POST".equals(method) || "GET".equals(method)) {
            exchange.sendResponseHeaders(404, -1); // Not Found for invalid path
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }
}