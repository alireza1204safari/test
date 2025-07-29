package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.ResponseMessage;
import dto.restaurant.RequestCreateRestaurant;
import dto.restaurant.ResponseRestaurant;
import entity.ResStatus;
import entity.Restaurant;
import entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import repository.RestaurantDao;
import repository.UserDao;
import util.HibernateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class RestaurantService extends BaseService {
    private final RestaurantDao restaurantDao;
    private final UserDao userDao;

    public RestaurantService() {
        super();
        this.restaurantDao = new RestaurantDao();
        this.userDao = new UserDao();
    }

    public void handleCreateRestaurant(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedVendor(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User vendor = userDao.getByPhone(phone);
            if (vendor == null) {
                sendResponse(exchange, new ErrorDto("User not found"), 404);
                return;
            }

            RequestCreateRestaurant request = getCreateRestaurantRequest(exchange);
            if (request.getPhone() != null && restaurantDao.isPhoneTaken(request.getPhone())) {
                sendResponse(exchange, new ErrorDto("Phone number already exists"), 409);
                return;
            }

            Transaction transaction = session.beginTransaction();
            Restaurant restaurant = new Restaurant();
            restaurant.setName(request.getName());
            restaurant.setAddress(request.getAddress());
            restaurant.setPhone(request.getPhone());
            restaurant.setLogoBase64(request.getLogoBase64());
            restaurant.setTaxFee(request.getTaxFee());
            restaurant.setAdditionalFee(request.getAdditional_fee());
            restaurant.setVendor(vendor);
            restaurant.setStatus(ResStatus.pending);

            restaurantDao.save(restaurant);
            transaction.commit();

            ResponseRestaurant response = new ResponseRestaurant(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getAddress(),
                    restaurant.getPhone(),
                    restaurant.getLogoBase64(),
                    restaurant.getTaxFee(),
                    restaurant.getAdditionalFee(),
                    restaurant.getStatus().name()
            );
            sendResponse(exchange, response, 201);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleGetMyRestaurants(HttpExchange exchange) throws IOException {
        try {
            String phone = isAuthorizedVendor(exchange);
            if (phone == null) return;


            List<Restaurant> restaurants = restaurantDao.getByVendorPhone(phone);
            List<ResponseRestaurant> response = restaurants.stream().map(r -> new ResponseRestaurant(
                    r.getId(),
                    r.getName(),
                    r.getAddress(),
                    r.getPhone(),
                    r.getLogoBase64(),
                    r.getTaxFee(),
                    r.getAdditionalFee(),
                    r.getStatus().name()
            )).collect(Collectors.toList());
            sendResponse(exchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleGetApprovedRestaurants(HttpExchange exchange) throws IOException {
        try {
            List<Restaurant> restaurants = restaurantDao.getApprovedRestaurants();
            List<ResponseRestaurant> response = restaurants.stream().map(r -> new ResponseRestaurant(
                    r.getId(),
                    r.getName(),
                    r.getAddress(),
                    r.getPhone(),
                    r.getLogoBase64(),
                    r.getTaxFee(),
                    r.getAdditionalFee(),
                    r.getStatus().name()
            )).collect(Collectors.toList());
            sendResponse(exchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleUpdateRestaurantStatus(HttpExchange exchange, Long restaurantId) throws IOException {
        String phone = isAuthorizedAdmin(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Restaurant restaurant = restaurantDao.getById(restaurantId);
            if (restaurant == null) {
                sendResponse(exchange, new ErrorDto("Restaurant not found"), 404);
                return;
            }

            JsonObject json = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.has("status") || json.get("status").isJsonNull() || json.get("status").getAsString().isBlank()) {
                sendResponse(exchange, new ErrorDto("Invalid input: status"), 400);
                return;
            }
            String statusStr = json.get("status").getAsString();
            ResStatus status;
            try {
                status = ResStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                sendResponse(exchange, new ErrorDto("Invalid status"), 400);
                return;
            }
            if (!List.of(ResStatus.approved, ResStatus.rejected).contains(status)) {
                sendResponse(exchange, new ErrorDto("Invalid status: Only approved or rejected allowed"), 400);
                return;
            }

            Transaction transaction = session.beginTransaction();
            restaurant.setStatus(status);
            restaurantDao.save(restaurant);
            transaction.commit();

            sendResponse(exchange, new ResponseMessage("Restaurant status updated successfully"), 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }


    public void handleGetPendingRestaurants(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedVendor(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Restaurant> restaurants = restaurantDao.getPendingRestaurants();
            List<ResponseRestaurant> response = restaurants.stream().map(r -> new ResponseRestaurant(
                    r.getId(),
                    r.getName(),
                    r.getAddress(),
                    r.getPhone(),
                    r.getLogoBase64(),
                    r.getTaxFee(),
                    r.getAdditionalFee(),
                    r.getStatus().name()
            )).collect(Collectors.toList());
            sendResponse(exchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleUpdateRestaurant(HttpExchange exchange, Long restaurantId) throws IOException {
        String phone = isAuthorizedVendor(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Restaurant restaurant = restaurantDao.getById(restaurantId);
            if (restaurant == null) {
                sendResponse(exchange, new ErrorDto("Restaurant not found"), 404);
                return;
            }
            if (!restaurant.getVendor().getPhone().equals(phone)) {
                sendResponse(exchange, new ErrorDto("Forbidden: Not your restaurant"), 403);
                return;
            }
            if (restaurant.getStatus() != ResStatus.pending) {
                sendResponse(exchange, new ErrorDto("Cannot update restaurant: Already approved or rejected"), 409);
                return;
            }

            RequestCreateRestaurant request = getCreateRestaurantRequest(exchange);
            if (request.getPhone() != null && restaurantDao.isPhoneTaken(request.getPhone()) && !request.getPhone().equals(restaurant.getPhone())) {
                sendResponse(exchange, new ErrorDto("Phone number already exists"), 409);
                return;
            }

            Transaction transaction = session.beginTransaction();
            if (request.getName() != null) restaurant.setName(request.getName());
            if (request.getAddress() != null) restaurant.setAddress(request.getAddress());
            if (request.getPhone() != null) restaurant.setPhone(request.getPhone());
            if (request.getLogoBase64() != null) restaurant.setLogoBase64(request.getLogoBase64());
            if (request.getTaxFee() != null) restaurant.setTaxFee(request.getTaxFee());
            if (request.getAdditional_fee() != null) restaurant.setAdditionalFee(request.getAdditional_fee());

            restaurantDao.save(restaurant);
            transaction.commit();

            ResponseRestaurant response = new ResponseRestaurant(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getAddress(),
                    restaurant.getPhone(),
                    restaurant.getLogoBase64(),
                    restaurant.getTaxFee(),
                    restaurant.getAdditionalFee(),
                    restaurant.getStatus().name()
            );
            sendResponse(exchange, response, 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    private RequestCreateRestaurant getCreateRestaurantRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("REQUEST BODY: " + body);
        Gson gson = new Gson();
        RequestCreateRestaurant request = gson.fromJson(body, RequestCreateRestaurant.class);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        String[] requiredFields = {"name", "address", "phone"};
        for (String field : requiredFields) {
            if (!json.has(field) || json.get(field).isJsonNull() || json.get(field).getAsString().isBlank()) {
                throw new IllegalArgumentException("Invalid input: " + field);
            }
        }
        return request;
    }
}