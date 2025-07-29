package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.CouponService;

import java.io.IOException;

public class CouponHandler implements HttpHandler {
    private final CouponService couponService;

    public CouponHandler() {
        this.couponService = new CouponService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/coupons") && "GET".equals(method)) {
            couponService.handleCheckCoupon(exchange);
            return;
        }

        // Handle invalid path or method
        if ("GET".equals(method)) {
            exchange.sendResponseHeaders(404, -1); // Not Found for invalid path
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }
}