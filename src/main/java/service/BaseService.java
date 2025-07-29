package service;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.ResponseMessage;
import entity.Role;
import entity.User;
import repository.UserDao;
import util.JwtUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

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
        Role role;
        try {
            role = Role.valueOf(roleStr.toLowerCase());
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto("Forbidden: Invalid role"), 403);
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
        Role role;
        try {
            role = Role.valueOf(roleStr.toLowerCase());
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto("Forbidden: Invalid role"), 403);
            return null;
        }

        if (role != Role.admin) {
            sendResponse(exchange, new ErrorDto("Forbidden: Only admins can perform this action"), 403);
            return null;
        }
        return phone;
    }


    protected void sendResponse(HttpExchange exchange, Object response, int status) throws IOException {
        String jsonResponse;
        if (response instanceof String) {
            jsonResponse = new Gson().toJson(new ResponseMessage((String) response));
        } else {
            jsonResponse = new Gson().toJson(response);
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
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
            sendResponse(exchange, new ErrorDto("Unauthorized: User not found or not a buyer"), 401);
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
            sendResponse(exchange, new ErrorDto("Unauthorized: User not found or not a seller"), 401);
            return null;
        }
        return phone;
    }}