package service;

import com.sun.net.httpserver.HttpExchange;
import dao.FavoriteDao;
import dao.FoodItemDao;
import dao.RestaurantDao;
import dao.UserDao;
import entity.Favorite;
import entity.Restaurant;
import entity.User;
import dto.ErrorDto;
import dto.ResponseMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FavoriteService extends BaseService {
    private final FavoriteDao favoriteDao = new FavoriteDao();
    private final UserDao userDao = new UserDao();
    private final RestaurantDao restaurantDao = new RestaurantDao();

    public FavoriteService() {
        super();
    }

    public void getFavorites(HttpExchange exchange) throws IOException {
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
        response.put("favorites_list", favoriteDao.getFavoritesByUserId(user.getId()));
        sendResponse(exchange, response, 200);
    }

    public void addFavorite(HttpExchange exchange, Map<String, Object> requestBody) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            return;
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Long itemId = Long.parseLong((String) requestBody.get("restaurantId"));
        Restaurant restaurant = restaurantDao.getById(itemId);
        if (restaurant == null) {
            sendResponse(exchange, new ErrorDto("restaurant not found"), 404);
            return;
        }
        Favorite existingFavorite = favoriteDao.getByUserIdAndItemId(user.getId(), itemId);
        if (existingFavorite != null) {
            sendResponse(exchange, new ErrorDto("Item already in favorites"), 409);
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setRestaurant(restaurant);
        favoriteDao.save(favorite);
        sendResponse(exchange, new ResponseMessage("Favorite added successfully"), 200);
    }

    public void removeFavorite(HttpExchange exchange, Long itemId) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            return;
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Favorite favorite = favoriteDao.getByUserIdAndItemId(user.getId(), itemId);
        if (favorite == null) {
            sendResponse(exchange, new ErrorDto("Favorite not found"), 404);
            return;
        }
        favoriteDao.delete(favorite);
        sendResponse(exchange, new ResponseMessage("Favorite removed successfully"), 200);
    }
}