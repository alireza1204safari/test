package service;

import com.sun.net.httpserver.HttpExchange;
import dao.AdminDao;
import entity.Coupon;
import entity.User;
import dto.ErrorDto;
import dto.ResponseMessage;
import entity.UserStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class AdminService extends BaseService {
    private final AdminDao adminDao = new AdminDao();

    public AdminService() {
        super();
    }

    public void getAllUsers(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("users_list", adminDao.getAllUsers());
        sendResponse(exchange, response, 200);
    }

    public void updateUserStatus(HttpExchange exchange, Long userId, Map<String, String> requestBody) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        User admin = adminDao.getUserById(userId);
        if (admin == null) {
            sendResponse(exchange, new ErrorDto("Admin not found"), 404);
            return;
        }
        User user = adminDao.getUserById(userId);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        String status = requestBody.get("status");
        if (status == null || status.isBlank() ) {
            sendResponse(exchange, new ErrorDto("Invalid status"), 400);
            return;
        }
        user.setStatus(UserStatus.valueOf(status));
        adminDao.updateUser(user);
        sendResponse(exchange, new ResponseMessage("User status updated successfully"), 200);
    }

    public void getAllOrders(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("orders_list", adminDao.getAllOrders());
        sendResponse(exchange, response, 200);
    }

    public void getAllTransactions(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("transactions_list", adminDao.getAllTransactions());
        sendResponse(exchange, response, 200);
    }

    public void createCoupon(HttpExchange exchange, Map<String, Object> requestBody) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        String couponCode = (String) requestBody.get("coupon_code");
        String type = (String) requestBody.get("type");

        Double valueDouble = (Double) requestBody.get("value");
        Integer value = valueDouble.intValue();

        Double minPriceDouble = (Double) requestBody.get("min_price");
        Integer minPrice = minPriceDouble.intValue();

        Double userCountDouble = (Double) requestBody.get("user_count");
        Integer userCount = userCountDouble.intValue();

        String startDateStr = (String) requestBody.get("start_date");
        String endDateStr = (String) requestBody.get("end_date");


        if (couponCode == null || type == null || startDateStr == null || endDateStr == null) {
            sendResponse(exchange, new ErrorDto("Missing required fields"), 400);
            return;
        }
        Coupon existingCoupon = adminDao.getCouponByCode(couponCode);
        if (existingCoupon != null) {
            sendResponse(exchange, new ErrorDto("Coupon code already exists"), 409);
            return;
        }
        Coupon coupon = new Coupon();
        coupon.setCouponCode(couponCode);
        coupon.setType(type);
        coupon.setValue(value);
        coupon.setMinPrice(minPrice);
        coupon.setUserCount(userCount);
        coupon.setStartDate(LocalDate.parse(startDateStr));
        coupon.setEndDate(LocalDate.parse(endDateStr));
        adminDao.saveCoupon(coupon);
        Map<String, Object> response = new HashMap<>();
        response.put("id", coupon.getId());
        sendResponse(exchange, response, 201);
    }

    public void getAllCoupons(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("coupons_list", adminDao.getAllCoupons());
        sendResponse(exchange, response, 200);
    }

    public void getCouponById(HttpExchange exchange, Long couponId) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        Coupon coupon = adminDao.getCouponById(couponId);
        if (coupon == null) {
            sendResponse(exchange, new ErrorDto("Coupon not found"), 404);
            return;
        }
        sendResponse(exchange, coupon, 200);
    }

    public void updateCoupon(HttpExchange exchange, Long couponId, Map<String, Object> requestBody) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        Coupon coupon = adminDao.getCouponById(couponId);
        if (coupon == null) {
            sendResponse(exchange, new ErrorDto("Coupon not found"), 404);
            return;
        }
        if (requestBody.containsKey("coupon_code")) {
            String newCode = (String) requestBody.get("coupon_code");
            Coupon existingCoupon = adminDao.getCouponByCode(newCode);
            if (existingCoupon != null && !existingCoupon.getId().equals(couponId)) {
                sendResponse(exchange, new ErrorDto("Coupon code already exists"), 409);
                return;
            }
            coupon.setCouponCode(newCode);
        }
        if (requestBody.containsKey("type")) {
            coupon.setType((String) requestBody.get("type"));
        }
        if (requestBody.containsKey("value")) {
            coupon.setValue((Integer) requestBody.get("value"));
        }
        if (requestBody.containsKey("min_price")) {
            coupon.setMinPrice((Integer) requestBody.get("min_price"));
        }
        if (requestBody.containsKey("user_count")) {
            coupon.setUserCount((Integer) requestBody.get("user_count"));
        }
        if (requestBody.containsKey("start_date")) {
            coupon.setStartDate(LocalDate.parse((String) requestBody.get("start_date")));
        }
        if (requestBody.containsKey("end_date")) {
            coupon.setEndDate(LocalDate.parse((String) requestBody.get("end_date")));
        }
        adminDao.saveCoupon(coupon);
        sendResponse(exchange, new ResponseMessage("Coupon updated successfully"), 200);
    }

    public void deleteCoupon(HttpExchange exchange, Long couponId) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) {
            return;
        }
        Coupon coupon = adminDao.getCouponById(couponId);
        if (coupon == null) {
            sendResponse(exchange, new ErrorDto("Coupon not found"), 404);
            return;
        }
        adminDao.deleteCoupon(coupon);
        sendResponse(exchange, new ResponseMessage("Coupon deleted successfully"), 200);
    }
}