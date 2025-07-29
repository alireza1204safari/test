package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.ResponseMessage;
import dto.restaurant.RequestAddMenuItem;
import dto.restaurant.ResponseMenu;
import entity.*;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import dao.FoodItemDao;
import dao.MenuDao;
import dao.RestaurantDao;
import util.HibernateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MenuService extends BaseService {
    private final MenuDao menuDao;
    private final RestaurantDao restaurantDao;
    private final FoodItemDao foodItemDao;

    public MenuService() {
        super();
        this.menuDao = new MenuDao();
        this.restaurantDao = new RestaurantDao();
        this.foodItemDao = new FoodItemDao();
    }

    public void handleAddMenu(HttpExchange exchange, Long restaurantId) throws IOException {
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
            if (restaurant.getVendor().getStatus() != UserStatus.approved) {
                sendResponse(exchange, new ErrorDto("Cannot add menu: Restaurant not approved"), 409);
                return;
            }

            JsonObject json = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.has("title") || json.get("title").isJsonNull() || json.get("title").getAsString().isBlank()) {
                sendResponse(exchange, new ErrorDto("Invalid input: title"), 400);
                return;
            }
            String title = json.get("title").getAsString();

            if (menuDao.getByTitleAndRestaurantId(title, restaurantId) != null) {
                sendResponse(exchange, new ErrorDto("Menu title already exists"), 409);
                return;
            }

            Transaction transaction = session.beginTransaction();
            Menu menu = new Menu();
            menu.setTitle(title);
            menu.setRestaurant(restaurant);
            menuDao.save(menu);
            transaction.commit();

            sendResponse(exchange, new ResponseMenu(title), 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleDeleteMenu(HttpExchange exchange, Long restaurantId, String title) throws IOException {
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
            if (restaurant.getVendor().getStatus() != UserStatus.approved) {
                sendResponse(exchange, new ErrorDto("Cannot delete menu: Restaurant not approved"), 409);
                return;
            }

            Menu menu = menuDao.getByTitleAndRestaurantId(title, restaurantId);
            if (menu == null) {
                sendResponse(exchange, new ErrorDto("Menu not found"), 404);
                return;
            }

            Transaction transaction = session.beginTransaction();
            menuDao.delete(menu);
            transaction.commit();

            sendResponse(exchange, new ResponseMessage("Food menu removed successfully"), 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleAddItemToMenu(HttpExchange exchange, Long restaurantId, String title) throws IOException {
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
            if (restaurant.getVendor().getStatus() != UserStatus.approved) {
                sendResponse(exchange, new ErrorDto("Cannot add item to menu: Restaurant not approved"), 409);
                return;
            }

            Menu menu = menuDao.getByTitleAndRestaurantId(title, restaurantId);
            if (menu == null) {
                sendResponse(exchange, new ErrorDto("Menu not found"), 404);
                return;
            }

            RequestAddMenuItem request = getAddMenuItemRequest(exchange);
            FoodItem foodItem = foodItemDao.getById(request.getItemId());
            if (foodItem == null || !foodItem.getRestaurant().getId().equals(restaurantId)) {
                sendResponse(exchange, new ErrorDto("Food item not found"), 404);
                return;
            }

            Transaction transaction = session.beginTransaction();
            // Initialize the items collection to avoid LazyInitializationException
            Hibernate.initialize(menu.getItems());
            menu.getItems().add(foodItem);
            menuDao.save(menu);
            transaction.commit();

            sendResponse(exchange, new ResponseMessage("Food item added to menu successfully"), 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleDeleteItemFromMenu(HttpExchange exchange, Long restaurantId, String title, Long itemId) throws IOException {
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
            if (restaurant.getVendor().getStatus() != UserStatus.approved) {
                sendResponse(exchange, new ErrorDto("Cannot delete item from menu: Restaurant not approved"), 409);
                return;
            }

            Menu menu = menuDao.getByTitleAndRestaurantId(title, restaurantId);
            if (menu == null) {
                sendResponse(exchange, new ErrorDto("Menu not found"), 404);
                return;
            }

            FoodItem foodItem = foodItemDao.getById(itemId);
            if (foodItem == null || !foodItem.getRestaurant().getId().equals(restaurantId)) {
                sendResponse(exchange, new ErrorDto("Food item not found"), 404);
                return;
            }

            Transaction transaction = session.beginTransaction();
            // Initialize the items collection to avoid LazyInitializationException
            Hibernate.initialize(menu.getItems());
            menu.getItems().remove(foodItem);
            menuDao.save(menu);
            transaction.commit();

            sendResponse(exchange, new ResponseMessage("Item removed from menu successfully"), 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    private RequestAddMenuItem getAddMenuItemRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("REQUEST BODY: " + body);
        Gson gson = new Gson();
        RequestAddMenuItem request = gson.fromJson(body, RequestAddMenuItem.class);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        if (!json.has("item_id") || json.get("item_id").isJsonNull()) {
            throw new IllegalArgumentException("Invalid input: item_id");
        }
        return request;
    }
}