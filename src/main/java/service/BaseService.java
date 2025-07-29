package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.ResponseMessage;
import entity.Role;
import entity.User;
import dao.UserDao;
import util.JwtUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public abstract class BaseService {
    protected final JwtUtil jwtUtil;

    public BaseService() {
        this.jwtUtil = new JwtUtil();
    }

    protected String isAuthorizedSeller(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Missing or invalid Authorization header"), 401);
            return null;
        }

        String token = authHeader.substring(7);
        String phone = jwtUtil.validateToken(token);
        if (phone == null) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Invalid or expired token"), 401);
            return null;
        }

        String roleStr = jwtUtil.getRoleFromToken(token);
        System.out.println("Role from token (seller): " + roleStr); // لوگ برای دیباگ
        Role role;
        try {
            role = Role.valueOf(roleStr != null ? roleStr.toLowerCase() : "");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto("Forbidden: Invalid role: " + roleStr), 403);
            return null;
        }

        if (role != Role.seller) {
            sendResponse(exchange, new ErrorDto("Forbidden: Only sellers can perform this action"), 403);
            return null;
        }
        return phone;
    }

    protected String isAuthorizedAdmin(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Missing or invalid Authorization header"), 401);
            return null;
        }

        String token = authHeader.substring(7);
        String phone = jwtUtil.validateToken(token);
        if (phone == null) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Invalid or expired token"), 401);
            return null;
        }

        String roleStr = jwtUtil.getRoleFromToken(token);
        System.out.println("Role from token (admin): " + roleStr); // لوگ برای دیباگ
        Role role;
        try {
            role = Role.valueOf(roleStr != null ? roleStr.toLowerCase() : "");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto("Forbidden: Invalid role: " + roleStr), 403);
            return null;
        }

        if (role != Role.admin) {
            sendResponse(exchange, new ErrorDto("Forbidden: Only admins can perform this action"), 403);
            return null;
        }
        return phone;
    }

    protected String isAuthorizedBuyer(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Bearer token required"), 401);
            return null;
        }
        String token = authHeader.substring(7);
        String phone = jwtUtil.validateToken(token);
        if (phone == null) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Invalid token"), 401);
            return null;
        }
        UserDao userDao = new UserDao();
        User user = userDao.getByPhone(phone);
        if (user == null || !user.getRole().equals(Role.buyer)) {
            sendResponse(exchange, new ErrorDto("Unauthorized: User not found or not a buyer"), 403);
            return null;
        }
        return phone;
    }

    protected String isAuthorizedVendor(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Bearer token required"), 401);
            return null;
        }
        String token = authHeader.substring(7);
        String phone = jwtUtil.validateToken(token);
        if (phone == null) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Invalid token"), 401);
            return null;
        }
        UserDao userDao = new UserDao();
        User user = userDao.getByPhone(phone);
        if (user == null || !user.getRole().equals(Role.seller)) {
            sendResponse(exchange, new ErrorDto("Unauthorized: User not found or not a seller"), 403);
            return null;
        }
        return phone;
    }

    protected String isAuthorizedCourier(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Bearer token required"), 401);
            return null;
        }
        String token = authHeader.substring(7);
        String phone = jwtUtil.validateToken(token);
        if (phone == null) {
            sendResponse(exchange, new ErrorDto("Unauthorized: Invalid token"), 401);
            return null;
        }
        UserDao userDao = new UserDao();
        User user = userDao.getByPhone(phone);
        if (user == null || !user.getRole().equals(Role.courier)) {
            sendResponse(exchange, new ErrorDto("Unauthorized: User not found or not a courier"), 403);
            return null;
        }
        return phone;
    }

    public void sendResponse(HttpExchange exchange, Object response, int status) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME; // پشتیبانی از نانوثانیه

                    @Override
                    public void write(JsonWriter out, LocalDateTime value) throws IOException {
                        if (value == null) {
                            out.nullValue();
                        } else {
                            // سریالایز به فرمت ISO 8601 با نانوثانیه
                            out.value(formatter.format(value));
                        }
                    }

                    @Override
                    public LocalDateTime read(JsonReader in) throws IOException {
                        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                            in.nextNull();
                            return null;
                        }
                        String dateStr = in.nextString();
                        try {
                            // پارس فرمت ISO 8601 با نانوثانیه
                            return LocalDateTime.parse(dateStr, formatter);
                        } catch (DateTimeParseException e1) {
                            try {
                                // اگه فقط تاریخ باشه (مثل "2025-07-29")
                                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                            } catch (DateTimeParseException e2) {
                                System.out.println("Failed to parse date: " + dateStr + ", error: " + e2.getMessage());
                                throw new IOException("Invalid date format: " + dateStr, e2);
                            }
                        }
                    }
                })
                .create();

        String jsonResponse;
        if (response instanceof String) {
            jsonResponse = gson.toJson(new ResponseMessage((String) response));
        } else {
            jsonResponse = gson.toJson(response);
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}