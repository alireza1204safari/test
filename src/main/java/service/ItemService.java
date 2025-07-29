package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.restaurant.ResponseFoodItem;
import entity.FoodItem;
import entity.ResStatus;
import entity.UserStatus;
import org.hibernate.Session;
import org.hibernate.query.Query;
import dao.FoodItemDao;
import util.HibernateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ItemService extends BaseService {
    private final FoodItemDao foodItemDao;

    public ItemService() {
        super();
        this.foodItemDao = new FoodItemDao();
    }

    public void handleListItems(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String search = null;
            Integer price = null;
            List<String> keywords = null;

            if (!body.isEmpty()) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("search") && !json.get("search").isJsonNull()) {
                    search = json.get("search").getAsString();
                }
                if (json.has("price") && !json.get("price").isJsonNull()) {
                    price = json.get("price").getAsInt();
                    if (price <= 0) {
                        sendResponse(exchange, new ErrorDto("Invalid input: price must be positive"), 400);
                        return;
                    }
                } else {
                    sendResponse(exchange, new ErrorDto("Invalid input: price is required"), 400);
                    return;
                }
                if (json.has("keywords") && !json.get("keywords").isJsonNull()) {
                    keywords = new Gson().fromJson(json.get("keywords"), List.class);
                }
            } else {
                sendResponse(exchange, new ErrorDto("Invalid input: request body is required with price"), 400);
                return;
            }

            StringBuilder hql = new StringBuilder("FROM FoodItem fi LEFT JOIN FETCH fi.keywords WHERE fi.restaurant.status = :status");
            hql.append(" AND fi.price <= :price");
            if (search != null && !search.isBlank() || keywords != null && !keywords.isEmpty()) {
                hql.append(" AND (");
                boolean hasCondition = false;
                if (search != null && !search.isBlank()) {
                    hql.append("(fi.name LIKE :search OR fi.description LIKE :search)");
                    hasCondition = true;
                }
                if (keywords != null && !keywords.isEmpty()) {
                    if (hasCondition) {
                        hql.append(" OR ");
                    }
                    hql.append("EXISTS (SELECT 1 FROM FoodItem fi2 JOIN fi2.keywords k WHERE fi2 = fi AND k IN :keywords)");
                }
                hql.append(")");
            }

            Query<FoodItem> query = session.createQuery(hql.toString(), FoodItem.class);
            query.setParameter("status", ResStatus.approved);
            query.setParameter("price", price.doubleValue()); // Convert to Double to match entity
            if (search != null && !search.isBlank()) {
                query.setParameter("search", "%" + search + "%");
            }
            if (keywords != null && !keywords.isEmpty()) {
                query.setParameter("keywords", keywords);
            }

            List<FoodItem> foodItems = query.list();
            List<ResponseFoodItem> response = foodItems.stream().map(fi -> new ResponseFoodItem(
                    fi.getId(),
                    fi.getName(),
                    fi.getDescription(),
                    fi.getPrice(),
                    fi.getSupply(),
                    fi.getImageBase64(),
                    fi.getKeywords()
            )).collect(Collectors.toList());

            sendResponse(exchange, response, 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleGetItemDetails(HttpExchange exchange, Long itemId) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            FoodItem foodItem = foodItemDao.getById(itemId);
            if (foodItem == null || foodItem.getRestaurant().getVendor().getStatus() != UserStatus.approved) {
                sendResponse(exchange, new ErrorDto("Item not found or not from an approved restaurant"), 404);
                return;
            }

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
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }
}