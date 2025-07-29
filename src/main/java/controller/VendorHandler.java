package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.VendorService;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VendorHandler implements HttpHandler {

    private final VendorService vendorService;

    public VendorHandler() {
        this.vendorService = new VendorService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("POST".equals(method)) {
            if (path.equals("/vendors")) {
                vendorService.handleListVendors(exchange);
                return;
            }
        } else if ("GET".equals(method)) {
            Pattern vendorPattern = Pattern.compile("^/vendors/(\\d+)$");
            Matcher vendorMatcher = vendorPattern.matcher(path);
            if (vendorMatcher.matches()) {
                Long vendorId = Long.parseLong(vendorMatcher.group(1));
                vendorService.handleGetVendorMenu(exchange, vendorId);
                return;
            }
        }

        // Handle invalid path or method
        if ("POST".equals(method) || "GET".equals(method)) {
            exchange.sendResponseHeaders(404, -1); // Not Found
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }
}