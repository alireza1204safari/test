package handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.ErrorDto;
import service.AdminService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AdminHandler implements HttpHandler {
    private final AdminService adminService;
    private final Gson gson;

    public AdminHandler() {
        this.adminService = new AdminService();
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/admin/users") && method.equals("GET")) {
                adminService.getAllUsers(exchange);
            } else if (path.matches("/admin/users/\\d+/status") && method.equals("PATCH")) {
                Long userId = Long.parseLong(path.split("/")[3]);
                Map<String, String> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                adminService.updateUserStatus(exchange, userId, requestBody);
            } else if (path.equals("/admin/orders") && method.equals("GET")) {
                adminService.getAllOrders(exchange);
            } else if (path.equals("/admin/transactions") && method.equals("GET")) {
                adminService.getAllTransactions(exchange);
            } else if (path.equals("/admin/coupons") && method.equals("POST")) {
                Map<String, Object> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                adminService.createCoupon(exchange, requestBody);
            } else if (path.equals("/admin/coupons") && method.equals("GET")) {
                adminService.getAllCoupons(exchange);
            } else if (path.matches("/admin/coupons/\\d+") && method.equals("GET")) {
                Long couponId = Long.parseLong(path.split("/")[3]);
                adminService.getCouponById(exchange, couponId);
            } else if (path.matches("/admin/coupons/\\d+") && method.equals("PUT")) {
                Long couponId = Long.parseLong(path.split("/")[3]);
                Map<String, Object> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                adminService.updateCoupon(exchange, couponId, requestBody);
            } else if (path.matches("/admin/coupons/\\d+") && method.equals("DELETE")) {
                Long couponId = Long.parseLong(path.split("/")[3]);
                adminService.deleteCoupon(exchange, couponId);
            } else {
                adminService.sendResponse(exchange, new ErrorDto("Not found"), 404);
            }
        } catch (Exception e) {
            adminService.sendResponse(exchange, new ErrorDto("Internal server error: " + e.getMessage()), 500);
        }
    }
}