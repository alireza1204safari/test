package service;

import com.sun.net.httpserver.HttpExchange;
import dao.OrderDao;
import dao.RatingDao;
import dao.UserDao;
import entity.Order;
import entity.Rating;
import entity.User;
import dto.ErrorDto;
import dto.ResponseMessage;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RatingService extends BaseService {
    private final RatingDao ratingDao = new RatingDao();
    private final UserDao userDao = new UserDao();
    private final OrderDao orderDao = new OrderDao();

    public RatingService() {
        super();
    }

    public void addRating(HttpExchange exchange, Map<String, Object> requestBody) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            return;
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Double orderIdDouble = (Double) requestBody.get("order_id");
        Long orderId = orderIdDouble.longValue();

        Double ratingDouble = (Double) requestBody.get("rating");
        Integer ratingValue = ratingDouble.intValue();
        String comment = (String) requestBody.get("comment");
        if (ratingValue < 1 || ratingValue > 5) {
            sendResponse(exchange, new ErrorDto("Invalid rating value"), 400);
            return;
        }
        Order order = orderDao.getById(orderId);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            sendResponse(exchange, new ErrorDto("Order not found or not owned by user"), 404);
            return;
        }
        Rating existingRating = ratingDao.getById(orderId);
        if (existingRating != null) {
            sendResponse(exchange, new ErrorDto("Rating already exists for this order"), 409);
            return;
        }
        Rating rating = new Rating();
        rating.setUser(user);
        rating.setOrder(order);
        rating.setRating(ratingValue);
        rating.setComment(comment);
        rating.setCreatedAt(new Date(System.currentTimeMillis()));
        ratingDao.save(rating);
        sendResponse(exchange, new ResponseMessage("Rating added successfully"), 200);
    }

    public void getRatings(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            return;
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("ratings_list", ratingDao.getByUserId(user.getId()));
        sendResponse(exchange, response, 200);
    }

    public void updateRating(HttpExchange exchange, Long ratingId, Map<String, Object> requestBody) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            return;
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Rating rating = ratingDao.getById(ratingId);
        if (rating == null || !rating.getUser().getId().equals(user.getId())) {
            sendResponse(exchange, new ErrorDto("Rating not found or not owned by user"), 404);
            return;
        }
        Integer ratingValue = (Integer) requestBody.get("rating");
        if (ratingValue != null) {
            if (ratingValue < 1 || ratingValue > 5) {
                sendResponse(exchange, new ErrorDto("Invalid rating value"), 400);
                return;
            }
            rating.setRating(ratingValue);
        }
        if (requestBody.containsKey("comment")) {
            rating.setComment((String) requestBody.get("comment"));
        }
        ratingDao.save(rating);
        sendResponse(exchange, new ResponseMessage("Rating updated successfully"), 200);
    }

    public void deleteRating(HttpExchange exchange, Long ratingId) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            return;
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Rating rating = ratingDao.getById(ratingId);
        if (rating == null || !rating.getUser().getId().equals(user.getId())) {
            sendResponse(exchange, new ErrorDto("Rating not found or not owned by user"), 404);
            return;
        }
        ratingDao.delete(rating);
        sendResponse(exchange, new ResponseMessage("Rating deleted successfully"), 200);
    }

    public void getRatingsByItemId(HttpExchange exchange, Long itemId) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("ratings_list", ratingDao.getByItemId(itemId));
        sendResponse(exchange, response, 200);
    }
}