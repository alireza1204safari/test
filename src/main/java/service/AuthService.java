package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dto.*;
import dto.auth.RequestLogin;
import dto.auth.ResponseLogin;
import dto.auth.ResponseSignUp;
import entity.BankInfo;
import entity.Role;
import entity.Token;
import entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import dao.TokenDao;
import dao.UserDao;
import util.HibernateUtil;
import util.JwtUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AuthService {
    private UserDao userDao;
    private JwtUtil jwtUtil;
    private TokenDao tokenDao;

    public AuthService() {
        this.userDao = new UserDao();
        this.jwtUtil = new JwtUtil();
        this.tokenDao = new TokenDao();
    }

    public void handleSignup(HttpExchange exchange) throws IOException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = getUserFromSignUpReq(exchange);
            Transaction transaction = session.beginTransaction();
            if (userDao.isPhoneTaken(user.getPhone())) {
                sendResponse(exchange, new ErrorDto("Phone number already exists"), 409);
                return;
            }
            BankInfo bankInfo = new BankInfo();
            session.save(bankInfo);
            user.setBankInfo(bankInfo);
            session.save(user);
            String token = jwtUtil.generateToken(user.getPhone(), user.getRole().toString());
            transaction.commit();
            ResponseSignUp response = new ResponseSignUp("User Created Successfully!", user.getId().toString(), token);
            sendResponse(exchange, response, 200);
        } catch (IllegalArgumentException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, "Failed to create user", 500);
        }
    }

    private void sendResponse(HttpExchange exchange, Object response, int status) throws IOException {
        String jsonResponse = new Gson().toJson(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private User getUserFromSignUpReq(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("REQUEST BODY: " + body);
        Gson gson = new Gson();
        User user = gson.fromJson(body, User.class);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String[] requiredFields = {"full_name", "phone", "password", "role"};
        if (user.getEmail() != null && !isValidEmail(user.getEmail())){
            sendResponse(exchange, "Invalid email", 400);
            throw new IllegalArgumentException("Invalid email");
        }
        for (String field : requiredFields) {
            if (!json.has(field) || json.get(field).isJsonNull() || json.get(field).getAsString().isBlank()) {
                sendResponse(exchange, "Invalid Input", 400);
                throw new IllegalArgumentException("Invalid input: " + field);
            }
        }

        user.setUsername(json.get("full_name").getAsString());
        user.setRole(Role.valueOf(json.getAsJsonObject().get("role").getAsString()));
        return user;
    }
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email.matches(emailRegex);
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            RequestLogin loginUser = getUserFromLoginReq(exchange);
            Transaction transaction = session.beginTransaction();
            User user = userDao.getByPhone(loginUser.getPhone());
            if (user == null || !user.getPassword().equals(loginUser.getPassword())) {
                String response = "Invalid username or password!";
                sendResponse(exchange, response, 401);
                return;
            }
            String tokent = jwtUtil.generateToken(user.getPhone(), user.getRole().toString());
            transaction.commit();
            ResponseLogin responseLogin = new ResponseLogin("User logged in successfully", mapUser(user), tokent);
            sendResponse(exchange, responseLogin, 200);
        } catch (IllegalArgumentException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
            String response = "Failed to login";
            sendResponse(exchange, response, 500);
        }
    }

    private RequestLogin getUserFromLoginReq(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("REQUEST BODY: " + body);
        Gson gson = new Gson();
        RequestLogin user = gson.fromJson(body, RequestLogin.class);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String[] requiredFields = {"phone", "password"};
        for (String field : requiredFields) {
            if (!json.has(field) || json.get(field).isJsonNull() || json.get(field).getAsString().isBlank()) {
                sendResponse(exchange, "Invalid Input", 400);
                throw new IllegalArgumentException("Invalid input: " + field);
            }
        }
        return user;
    }

    private ResponseUser mapUser(User user) {
        if (user == null) return null;
        return new ResponseUser(
                user.getEmail(),
                user.getUsername(),
                user.getRole().name(),
                user.getFull_name(),
                user.getPhone(),
                user.getAddress(),
                user.getProfileImageBase64(),
                mapBankInfo(user.getBankInfo())
        );
    }
    private ResponseBankInfo mapBankInfo(BankInfo bankInfo){
        return new ResponseBankInfo(bankInfo.getAccountNumber()
                ,bankInfo.getBankName());
    }


    public void handleLogout(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, new ErrorDto("Unauthorized"), 401);
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix
        String phone = jwtUtil.validateToken(token);
        if (phone == null) {
            sendResponse(exchange, new ErrorDto("Invalid token"), 401);
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            Token invalidatedToken = new Token(token);
            tokenDao.save(invalidatedToken);
            transaction.commit();
            sendResponse(exchange, "User logged out successfully", 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }
}
