package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.FoodItemService;
import service.MenuService;
import service.OrderService;
import service.RestaurantService;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestaurantHandler implements HttpHandler {

    private final RestaurantService restaurantService;
    private final FoodItemService foodItemService;
    private final MenuService menuService;
    private final OrderService orderService;

    public RestaurantHandler() {
        this.restaurantService = new RestaurantService();
        this.foodItemService = new FoodItemService();
        this.menuService = new MenuService();
        this.orderService = new OrderService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // /restaurants
        if (path.equals("/restaurants") && "POST".equals(method)) {
            restaurantService.handleCreateRestaurant(exchange);
            return;
        }

        // /restaurants/mine
        if (path.equals("/restaurants/mine") && "GET".equals(method)) {
            restaurantService.handleGetMyRestaurants(exchange);
            return;
        }

        // /restaurants/{id}
        Pattern restaurantPattern = Pattern.compile("^/restaurants/(\\d+)$");
        Matcher restaurantMatcher = restaurantPattern.matcher(path);
        if (restaurantMatcher.matches() && "PUT".equals(method)) {
            Long restaurantId = Long.parseLong(restaurantMatcher.group(1));
            restaurantService.handleUpdateRestaurant(exchange, restaurantId);
            return;
        }

        // /restaurants/{id}/item
        Pattern itemPattern = Pattern.compile("^/restaurants/(\\d+)/item$");
        Matcher itemMatcher = itemPattern.matcher(path);
        if (itemMatcher.matches() && "POST".equals(method)) {
            Long restaurantId = Long.parseLong(itemMatcher.group(1));
            foodItemService.handleAddFoodItem(exchange, restaurantId);
            return;
        }

        // /restaurants/{id}/item/{item_id}
        Pattern itemEditPattern = Pattern.compile("^/restaurants/(\\d+)/item/(\\d+)$");
        Matcher itemEditMatcher = itemEditPattern.matcher(path);
        if (itemEditMatcher.matches()) {
            Long restaurantId = Long.parseLong(itemEditMatcher.group(1));
            Long itemId = Long.parseLong(itemEditMatcher.group(2));
            if ("PUT".equals(method)) {
                foodItemService.handleUpdateFoodItem(exchange, restaurantId, itemId);
                return;
            } else if ("DELETE".equals(method)) {
                foodItemService.handleDeleteFoodItem(exchange, restaurantId, itemId);
                return;
            }
        }

        // /restaurants/{id}/menu
        Pattern menuPattern = Pattern.compile("^/restaurants/(\\d+)/menu$");
        Matcher menuMatcher = menuPattern.matcher(path);
        if (menuMatcher.matches() && "POST".equals(method)) {
            Long restaurantId = Long.parseLong(menuMatcher.group(1));
            menuService.handleAddMenu(exchange, restaurantId);
            return;
        }

        // /restaurants/{id}/menu/{title}
        Pattern menuEditPattern = Pattern.compile("^/restaurants/(\\d+)/menu/([^/]+)$");
        Matcher menuEditMatcher = menuEditPattern.matcher(path);
        if (menuEditMatcher.matches()) {
            Long restaurantId = Long.parseLong(menuEditMatcher.group(1));
            String title = menuEditMatcher.group(2);
            if ("DELETE".equals(method)) {
                menuService.handleDeleteMenu(exchange, restaurantId, title);
                return;
            } else if ("PUT".equals(method)) {
                menuService.handleAddItemToMenu(exchange, restaurantId, title);
                return;
            }
        }

        // /restaurants/{id}/menu/{title}/{item_id}
        Pattern menuItemPattern = Pattern.compile("^/restaurants/(\\d+)/menu/([^/]+)/(\\d+)$");
        Matcher menuItemMatcher = menuItemPattern.matcher(path);
        if (menuItemMatcher.matches() && "DELETE".equals(method)) {
            Long restaurantId = Long.parseLong(menuItemMatcher.group(1));
            String title = menuItemMatcher.group(2);
            Long itemId = Long.parseLong(menuItemMatcher.group(3));
            menuService.handleDeleteItemFromMenu(exchange, restaurantId, title, itemId);
            return;
        }

        // /restaurants/{id}/orders
        Pattern ordersPattern = Pattern.compile("^/restaurants/(\\d+)/orders$");
        Matcher ordersMatcher = ordersPattern.matcher(path);
        if (ordersMatcher.matches() && "GET".equals(method)) {
            Long restaurantId = Long.parseLong(ordersMatcher.group(1));
            String status = exchange.getRequestURI().getQuery() != null ?
                    getQueryParam(exchange, "status") : null;
            String search = getQueryParam(exchange, "search");
            String username = getQueryParam(exchange, "user");
            String couriername = getQueryParam(exchange, "courier");
            orderService.handleGetOrders(exchange, restaurantId, status, search, username, couriername);
            return;
        }

        Pattern orderPattern = Pattern.compile("^/restaurants/orders/(\\d+)$");
        Matcher orderMatcher = orderPattern.matcher(path);
        if (orderMatcher.matches() && "PATCH".equals(method)) {
            Long orderId = Long.parseLong(orderMatcher.group(1));
            orderService.handleUpdateOrderStatus(exchange, orderId);
            return;
        }

        exchange.sendResponseHeaders(404, -1); // Not Found
    }

    private String getQueryParam(HttpExchange exchange, String param) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length == 2 && parts[0].equals(param)) {
                return parts[1];
            }
        }
        return null;
    }
}