package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.ResponseBankInfo;
import dto.ResponseUser;
import dto.profile.RequestUpdateProfile;
import dto.profile.ResponseProfile;
import entity.BankInfo;
import entity.Role;
import entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import repository.UserDao;
import util.HibernateUtil;
import util.JwtUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ProfileService {

    private final UserDao userDao;
    private final JwtUtil jwtUtil;

    public ProfileService() {
        this.userDao = new UserDao();
        this.jwtUtil = new JwtUtil();
    }

    public void handleGetProfile(HttpExchange exchange) throws IOException {
        try{
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


            User user = userDao.getByPhone(phone);
            if (user == null) {
                sendResponse(exchange, new ErrorDto("User not found"), 404);
                return;
            }

            ResponseUser response = mapUserToProfile(user);
            sendResponse(exchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleUpdateProfile(HttpExchange exchange) throws IOException {
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
            User user = userDao.getByPhone(phone);
            if (user == null) {
                sendResponse(exchange, new ErrorDto("User not found"), 404);
                return;
            }

            RequestUpdateProfile request = getProfileFromRequest(exchange);
            Transaction transaction = session.beginTransaction();

            // Update user fields
            if (request.getFullName() != null) {
                user.setFull_name(request.getFullName());
            }
            if (request.getPhone() != null) {
                if (userDao.isPhoneTaken(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
                    sendResponse(exchange, new ErrorDto("Phone number already exists"), 409);
                    return;
                }
                user.setPhone(request.getPhone());
            }
            if (request.getEmail() != null) {
                user.setUsername(request.getEmail());
            }
            if (request.getAddress() != null) {
                user.setAddress(request.getAddress());
            }
            if (request.getProfileImageBase64() != null) {
                user.setProfileImageBase64(request.getProfileImageBase64());
            }

            // Update bank info only for seller or delivery roles
            if (user.getRole() == Role.seller || user.getRole() == Role.courier) {
                if (request.getBankName() != null || request.getAccountNumber() != null) {
                    BankInfo bankInfo = user.getBankInfo();
                    if (bankInfo == null) {
                        bankInfo = new BankInfo();
                        user.setBankInfo(bankInfo);
                    }
                    if (request.getBankName() != null) {
                        bankInfo.setBankName(request.getBankName());
                    }
                    if (request.getAccountNumber() != null) {
                        bankInfo.setAccountNumber(request.getAccountNumber());
                    }
                }
            }

            session.update(user);
            transaction.commit();
            sendResponse(exchange, new ResponseProfile("Profile updated successfully",
                                                        mapUserToProfile(user)), 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    private RequestUpdateProfile getProfileFromRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("REQUEST BODY: " + body);
        Gson gson = new Gson();
        RequestUpdateProfile request = gson.fromJson(body, RequestUpdateProfile.class);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        if (json.has("bank_info")) {
            JsonObject bankInfo = json.getAsJsonObject("bank_info");
            String[] requiredBankFields = {"bank_name", "account_number"};
            for (String field : requiredBankFields) {
                if (!bankInfo.has(field) || bankInfo.get(field).isJsonNull() || bankInfo.get(field).getAsString().isBlank()) {
                    throw new IllegalArgumentException("Invalid input: " + field);
                }
            }
            request.setAccountNumber(json.get("bank_info").getAsJsonObject().get("account_number").getAsString());
            request.setBankName(json.get("bank_info").getAsJsonObject().get("bank_name").getAsString());
        }

        return request;
    }

    private ResponseUser mapUserToProfile(User user) {
        return new ResponseUser(
                user.getEmail(),
                user.getUsername(),
                user.getRole().toString(),
                user.getFull_name(),
                user.getPhone() ,
                user.getAddress(),
                user.getProfileImageBase64(),
                mapBankInfo(user.getBankInfo())
        );
    }

    private ResponseBankInfo mapBankInfo(BankInfo bankInfo){
        return new ResponseBankInfo(bankInfo.getAccountNumber()
        ,bankInfo.getBankName());
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
}