package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.restaurant.ResponseFoodItem;
import dto.restaurant.ResponseRestaurant;
import entity.Menu;
import entity.ResStatus;
import entity.Restaurant;
import entity.UserStatus;
import org.hibernate.Session;
import org.hibernate.query.Query;
import dao.MenuDao;
import dao.RestaurantDao;
import util.HibernateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VendorService extends BaseService {
    private final RestaurantDao restaurantDao;
    private final MenuDao menuDao;

    public VendorService() {
        super();
        this.restaurantDao = new RestaurantDao();
        this.menuDao = new MenuDao();
    }

    public void handleListVendors(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String search = null;
            List<String> keywords = null;

            if (!body.isEmpty()) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("search") && !json.get("search").isJsonNull()) {
                    search = json.get("search").getAsString();
                }
                if (json.has("keywords") && !json.get("keywords").isJsonNull()) {
                    keywords = new Gson().fromJson(json.get("keywords"), List.class);
                }
            }

            StringBuilder hql = new StringBuilder("FROM Restaurant r WHERE r.vendor.status = :status");
            boolean tmp = false;
            if (search != null && !search.isBlank()) {
                hql.append(" AND r.name LIKE :search");
                tmp = true;
            }
            if (keywords != null && !keywords.isEmpty()) {
                hql.append(tmp ? " OR " : "AND").append("EXISTS (SELECT fi FROM FoodItem fi JOIN fi.keywords k WHERE fi.restaurant = r AND k IN :keywords)");
            }


            Query<Restaurant> query = session.createQuery(hql.toString(), Restaurant.class);
            query.setParameter("status", UserStatus.approved);
            if (search != null && !search.isBlank()) {
                query.setParameter("search", "%" + search + "%");
            }
            if (keywords != null && !keywords.isEmpty()) {
                query.setParameter("keywords", keywords);
            }

            List<Restaurant> restaurants = query.list();
            List<ResponseRestaurant> response = restaurants.stream().map(r -> new ResponseRestaurant(
                    r.getId(),
                    r.getName(),
                    r.getAddress(),
                    r.getPhone(),
                    r.getLogoBase64(),
                    r.getTaxFee(),
                    r.getAdditionalFee()
            )).collect(Collectors.toList());

            sendResponse(exchange, response, 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleGetVendorMenu(HttpExchange exchange, Long vendorId) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Restaurant restaurant = restaurantDao.getById(vendorId);
            if (restaurant == null || restaurant.getVendor().getStatus() != UserStatus.approved) {
                sendResponse(exchange, new ErrorDto("Vendor not found or not approved"), 404);
                return;
            }

            List<Menu> menus = menuDao.getByRestaurantId(vendorId);
            Map<String, Object> response = new HashMap<>();
            ResponseRestaurant vendorResponse = new ResponseRestaurant(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getAddress(),
                    restaurant.getPhone(),
                    restaurant.getLogoBase64(),
                    restaurant.getTaxFee(),
                    restaurant.getAdditionalFee()
            );
            response.put("vendor", vendorResponse);

            List<String> menuTitles = menus.stream().map(Menu::getTitle).collect(Collectors.toList());
            response.put("menu_titles", menuTitles);

            Map<String, List<ResponseFoodItem>> menuItems = new HashMap<>();
            for (Menu menu : menus) {
                session.refresh(menu); // Ensure items are loaded
                List<ResponseFoodItem> items = menu.getItems().stream().map(fi -> new ResponseFoodItem(
                        fi.getId(),
                        fi.getName(),
                        fi.getDescription(),
                        fi.getPrice(),
                        fi.getSupply(),
                        fi.getImageBase64(),
                        fi.getKeywords()
                )).collect(Collectors.toList());
                menuItems.put(menu.getTitle(), items);
            }
            response.put("menu_title", menuItems);

            sendResponse(exchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }
}