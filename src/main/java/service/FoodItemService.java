package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.ResponseMessage;
import dto.restaurant.RequestCreateFoodItem;
import dto.restaurant.ResponseFoodItem;
import dto.restaurant.RequestUpdateFoodItem;
import entity.FoodItem;
import entity.ResStatus;
import entity.Restaurant;
import org.hibernate.Session;
import org.hibernate.Transaction;
import repository.FoodItemDao;
import repository.RestaurantDao;
import util.HibernateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FoodItemService extends BaseService {
    private final FoodItemDao foodItemDao;
    private final RestaurantDao restaurantDao;

    public FoodItemService() {
        super();
        this.foodItemDao = new FoodItemDao();
        this.restaurantDao = new RestaurantDao();
    }

    public void handleAddFoodItem(HttpExchange exchange, Long restaurantId) throws IOException {
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
            if (restaurant.getStatus() != ResStatus.approved) {
                sendResponse(exchange, new ErrorDto("Cannot add food item: Restaurant not approved"), 409);
                return;
            }

            RequestCreateFoodItem request = getCreateFoodItemRequest(exchange);
            // Validate vendorId matches restaurant's vendor
            if (request.getVendorId() == null) {
                sendResponse(exchange, new ErrorDto("Invalid input: vendor_id is required"), 400);
                return;
            }
            if (!request.getVendorId().equals(restaurant.getVendor().getId())) {
                sendResponse(exchange, new ErrorDto("Invalid vendor_id: Does not match restaurant's vendor"), 403);
                return;
            }

            Transaction transaction = session.beginTransaction();
            FoodItem foodItem = new FoodItem();
            foodItem.setName(request.getName());
            foodItem.setDescription(request.getDescription());
            foodItem.setPrice(request.getPrice());
            foodItem.setSupply(request.getSupply());
            foodItem.setImageBase64(request.getImageBase64());
            foodItem.setKeywords(request.getKeywords());
            foodItem.setRestaurant(restaurant);

            foodItemDao.save(foodItem);
            transaction.commit();

            ResponseFoodItem response = new ResponseFoodItem(
                    foodItem.getId(),
                    foodItem.getName(),
                    foodItem.getDescription(),
                    foodItem.getPrice(),
                    foodItem.getSupply(),
                    foodItem.getImageBase64(),
                    foodItem.getKeywords()
            );
            sendResponse(exchange, response, 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleUpdateFoodItem(HttpExchange exchange, Long restaurantId, Long itemId) throws IOException {
        String phone = isAuthorizedSeller(exchange);
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
            if (restaurant.getStatus() != ResStatus.approved) {
                sendResponse(exchange, new ErrorDto("Cannot update food item: Restaurant not approved"), 409);
                return;
            }

            FoodItem foodItem = foodItemDao.getById(itemId);
            if (foodItem == null || !foodItem.getRestaurant().getId().equals(restaurantId)) {
                sendResponse(exchange, new ErrorDto("Food item not found"), 404);
                return;
            }

            RequestUpdateFoodItem request = getUpdateFoodItemRequest(exchange);
            Transaction transaction = session.beginTransaction();
            if (request.getName() != null) foodItem.setName(request.getName());
            if (request.getDescription() != null) foodItem.setDescription(request.getDescription());
            if (request.getPrice() != null) foodItem.setPrice(request.getPrice());
            if (request.getSupply() != null) foodItem.setSupply(request.getSupply());
            if (request.getImageBase64() != null) foodItem.setImageBase64(request.getImageBase64());
            if (request.getKeywords() != null) foodItem.setKeywords(request.getKeywords());

            foodItemDao.save(foodItem);
            transaction.commit();

            ResponseFoodItem response = new ResponseFoodItem(
                    foodItem.getId(),
                    foodItem.getName(),
                    foodItem.getDescription(),
                    foodItem.getPrice(),
                    foodItem.getSupply(),
                    foodItem.getImageBase64(),
                    foodItem.getKeywords()
            );
            sendResponse(exchange, response, 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleDeleteFoodItem(HttpExchange exchange, Long restaurantId, Long itemId) throws IOException {
        String phone = isAuthorizedSeller(exchange);
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
            if (restaurant.getStatus() != ResStatus.approved) {
                sendResponse(exchange, new ErrorDto("Cannot delete food item: Restaurant not approved"), 409);
                return;
            }

            FoodItem foodItem = foodItemDao.getById(itemId);
            if (foodItem == null || !foodItem.getRestaurant().getId().equals(restaurantId)) {
                sendResponse(exchange, new ErrorDto("Food item not found"), 404);
                return;
            }

            Transaction transaction = session.beginTransaction();
            foodItemDao.delete(foodItem);
            transaction.commit();

            sendResponse(exchange, new ResponseMessage("Food item removed successfully"), 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }
    private RequestUpdateFoodItem getUpdateFoodItemRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("REQUEST BODY: " + body);
        Gson gson = new Gson();
        RequestUpdateFoodItem request = gson.fromJson(body, RequestUpdateFoodItem.class);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        String[] requiredFields = {"name", "description", "price", "supply", "keywords"};
        for (String field : requiredFields) {
            if (!json.has(field) || json.get(field).isJsonNull() ||
                    (field.equals("keywords") ? json.get(field).getAsJsonArray().size() == 0 :
                             json.get(field).getAsString().isBlank())) {
                throw new IllegalArgumentException("Invalid input: " + field);
            }
        }
        return request;
    }


    private RequestCreateFoodItem getCreateFoodItemRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("REQUEST BODY: " + body);
        Gson gson = new Gson();
        RequestCreateFoodItem request = gson.fromJson(body, RequestCreateFoodItem.class);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        String[] requiredFields = {"name", "description", "price", "supply", "keywords", "vendor_id"};
        for (String field : requiredFields) {
            if (!json.has(field) || json.get(field).isJsonNull() ||
                    (field.equals("keywords") ? json.get(field).getAsJsonArray().size() == 0 :
                            field.equals("vendor_id") ? json.get(field).getAsLong() <= 0 : json.get(field).getAsString().isBlank())) {
                throw new IllegalArgumentException("Invalid input: " + field);
            }
        }
        return request;
    }
}