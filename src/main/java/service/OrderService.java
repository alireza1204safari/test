package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.restaurant.ResponseOrder;
import entity.*;
import org.hibernate.Session;
import org.hibernate.query.Query;
import repository.*;
import util.HibernateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class OrderService extends BaseService {
    private final OrderDao orderDao;
    private final RestaurantDao restaurantDao;
    private final FoodItemDao foodItemDao;
    private final CouponDao couponDao;
    private final UserDao userDao;

    public OrderService() {
        super();
        this.orderDao = new OrderDao();
        this.restaurantDao = new RestaurantDao();
        this.foodItemDao = new FoodItemDao();
        this.couponDao = new CouponDao();
        this.userDao = new UserDao();
    }

    public void handleSubmitOrder(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (body.isEmpty()) {
                sendResponse(exchange, new ErrorDto("Invalid input: request body is required"), 400);
                return;
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("delivery_address") || json.get("delivery_address").isJsonNull() ||
                    !json.has("vendor_id") || json.get("vendor_id").isJsonNull() ||
                    !json.has("items") || json.get("items").isJsonNull()) {
                sendResponse(exchange, new ErrorDto("Invalid input: delivery_address, vendor_id, and items are required"), 400);
                return;
            }

            String deliveryAddress = json.get("delivery_address").getAsString();
            Long vendorId = json.get("vendor_id").getAsLong();
            Long couponId = json.has("coupon_id") && !json.get("coupon_id").isJsonNull() ? json.get("coupon_id").getAsLong() : null;
            JsonArray itemsArray = json.getAsJsonArray("items");

            if (deliveryAddress.isBlank()) {
                sendResponse(exchange, new ErrorDto("Invalid input: delivery_address cannot be empty"), 400);
                return;
            }
            if (itemsArray.isEmpty()) {
                sendResponse(exchange, new ErrorDto("Invalid input: items array cannot be empty"), 400);
                return;
            }

            // Validate items
            List<OrderItem> orderItems = new ArrayList<>();
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject item = itemsArray.get(i).getAsJsonObject();
                if (!item.has("item_id") || item.get("item_id").isJsonNull() ||
                        !item.has("quantity") || item.get("quantity").isJsonNull()) {
                    sendResponse(exchange, new ErrorDto("Invalid input: item_id and quantity are required for each item"), 400);
                    return;
                }
                Long itemId = item.get("item_id").getAsLong();
                Integer quantity = item.get("quantity").getAsInt();
                if (quantity <= 0) {
                    sendResponse(exchange, new ErrorDto("Invalid input: quantity must be positive"), 400);
                    return;
                }
                FoodItem foodItem = foodItemDao.getById(itemId);
                if (foodItem == null || foodItem.getRestaurant().getId() != vendorId || foodItem.getSupply() < quantity) {
                    sendResponse(exchange, new ErrorDto("Invalid input: item not found, not from vendor, or insufficient supply"), 400);
                    return;
                }
                OrderItem orderItem = new OrderItem();
                orderItem.setFoodItem(foodItem);
                orderItem.setQuantity(quantity);
                orderItems.add(orderItem);
            }

            // Validate restaurant
            Restaurant vendor = restaurantDao.getById(vendorId);
            if (vendor == null || vendor.getStatus() != ResStatus.approved) {
                sendResponse(exchange, new ErrorDto("Restaurant not found or not approved"), 404);
                return;
            }

            // Validate coupon
            Coupon coupon = null;
            if (couponId != null) {
                coupon = couponDao.getByCouponCode(couponId.toString());
                if (coupon == null || coupon.getUserCount() <= 0 || coupon.getStartDate().isAfter(LocalDate.now()) ||
                        coupon.getEndDate().isBefore(LocalDate.now()) || !coupon.getType().matches("fixed|percent")) {
                    sendResponse(exchange, new ErrorDto("Invalid or expired coupon"), 400);
                    return;
                }
            }

            // Get customer
            User customer = userDao.getByPhone(phone);
            if (customer == null || !customer.getRole().equals(Role.buyer)) {
                sendResponse(exchange, new ErrorDto("Customer not found or not a buyer"), 404);
                return;
            }

            // Calculate prices
            int rawPrice = orderItems.stream().mapToInt(item -> (int) (item.getFoodItem().getPrice() * item.getQuantity())).sum();
            int taxFee = vendor.getTaxFee() != null ? vendor.getTaxFee() : 0;
            int additionalFee = vendor.getAdditionalFee() != null ? vendor.getAdditionalFee() : 0;
            int courierFee = 10; // Fixed courier fee (adjust as needed)
            int payPrice = rawPrice + taxFee + additionalFee + courierFee;
            if (coupon != null) {
                if (rawPrice < coupon.getMinPrice()) {
                    sendResponse(exchange, new ErrorDto("Order total is less than coupon minimum price"), 400);
                    return;
                }
                if (coupon.getType().equals("fixed")) {
                    payPrice -= coupon.getValue().intValue();
                } else if (coupon.getType().equals("percent")) {
                    payPrice -= (int) (rawPrice * (coupon.getValue() / 100));
                }
                if (payPrice < 0) payPrice = 0;
            }

            // Create order
            Order order = new Order();
            order.setDeliveryAddress(deliveryAddress);
            order.setUser(customer);
            order.setVendor(vendor);
            order.setCoupon(coupon);
            order.setItems(orderItems);
            order.setRawPrice(rawPrice);
            order.setTaxFee(taxFee);
            order.setAdditionalFee(additionalFee);
            order.setCourierFee(courierFee);
            order.setPayPrice(payPrice);
            order.setStatus(OrderStatus.submitted);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderItems.forEach(item -> item.setOrder(order));

            // Update supply
            for (OrderItem item : orderItems) {
                FoodItem foodItem = item.getFoodItem();
                foodItem.setSupply(foodItem.getSupply() - item.getQuantity());
                session.update(foodItem);
            }

            // Save order
            session.beginTransaction();
            session.save(order);
            session.getTransaction().commit();

            ResponseOrder response = new ResponseOrder(
                    order.getId(), order.getDeliveryAddress(), order.getUser().getId(),
                    order.getVendor().getId(), order.getCoupon() != null ? order.getCoupon().getId() : null,
                    order.getItems(), order.getRawPrice(), order.getTaxFee(), order.getAdditionalFee(),
                    order.getCourierFee(), order.getPayPrice(), order.getCourierId(),
                    order.getStatus().toString(), order.getCreatedAt(), order.getUpdatedAt()
            );

            sendResponse(exchange, response, 200);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, new ErrorDto(e.getMessage()), 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleGetOrderDetails(HttpExchange exchange, Long orderId) throws IOException {
        try {
            String phone = isAuthorizedBuyer(exchange);
            if (phone == null) return;


            Order order = orderDao.getById(orderId);
            if (order == null) {
                sendResponse(exchange, new ErrorDto("Order not found"), 404);
                return;
            }
            if (!order.getUser().getPhone().equals(phone)) {
                sendResponse(exchange, new ErrorDto("Unauthorized: order does not belong to this user"), 403);
                return;
            }

            ResponseOrder response = new ResponseOrder(
                    order.getId(), order.getDeliveryAddress(), order.getUser().getId(),
                    order.getVendor().getId(), order.getCoupon() != null ? order.getCoupon().getId() : null,
                    order.getItems(), order.getRawPrice(), order.getTaxFee(), order.getAdditionalFee(),
                    order.getCourierFee(), order.getPayPrice(), order.getCourierId(),
                    order.getStatus().toString(), order.getCreatedAt(), order.getUpdatedAt()
            );

            sendResponse(exchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleGetOrderHistory(HttpExchange exchange) throws IOException {
        try {
            String phone = isAuthorizedBuyer(exchange);
            if (phone == null) return;


            String queryString = exchange.getRequestURI().toString();
            String search = null;
            String vendor = null;

            if (queryString != null) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (param.contains("=")) {
                        String[] keyValue = param.split("=", 2); // Split into max 2 parts to handle values with =
                        String key = keyValue[0].toLowerCase();
                        String value = keyValue.length > 1 ? java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                        if ("search".equals(key)) {
                            search = value;
                        } else if ("vendor".equals(key)) {
                            vendor = value;
                        }
                    }
                }
            }

            User customer = userDao.getByPhone(phone);
            if (customer == null || !customer.getRole().equals(Role.buyer)) {
                sendResponse(exchange, new ErrorDto("Customer not found or not a buyer"), 404);
                return;
            }

            List<Order> orders = orderDao.getByCustomerId(customer.getId(), search, vendor);
            List<ResponseOrder> response = orders.stream().map(order -> new ResponseOrder(
                    order.getId(), order.getDeliveryAddress(), order.getUser().getId(),
                    order.getVendor().getId(), order.getCoupon() != null ? order.getCoupon().getId() : null,
                    order.getItems(), order.getRawPrice(), order.getTaxFee(), order.getAdditionalFee(),
                    order.getCourierFee(), order.getPayPrice(), order.getCourierId(),
                    order.getStatus().toString(), order.getCreatedAt(), order.getUpdatedAt()
            )).collect(Collectors.toList());

            sendResponse(exchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }

    public void handleGetOrders(HttpExchange exchange, Long restaurantId, String status, String search, String user, String courier) throws IOException {
        String phone = isAuthorizedVendor(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User seller = userDao.getByPhone(phone);
            if (seller == null || seller.getRole() != Role.seller) {
                sendResponse(exchange, new ErrorDto("Unauthorized: user not found or not a seller"), 403);
                return;
            }

            Restaurant restaurant = restaurantDao.getById(restaurantId);
            if (restaurant == null || !restaurant.getVendor().getId().equals(seller.getId())) {
                sendResponse(exchange, new ErrorDto("Restaurant not found or not owned by this seller"), 404);
                return;
            }

            if (restaurant.getStatus() != ResStatus.approved) {
                sendResponse(exchange, new ErrorDto("Restaurant is not approved"), 403);
                return;
            }

            StringBuilder hql = new StringBuilder(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN o.items i LEFT JOIN i.foodItem f " +
                            "LEFT JOIN o.user c LEFT JOIN o.vendor v WHERE v.id = :restaurantId"
            );

            boolean hasCondition = false;
            if (status != null && !status.isBlank()) {
                if (!EnumSet.allOf(OrderStatus.class).stream().map(Enum::name).collect(Collectors.toSet()).contains(status)) {
                    sendResponse(exchange, new ErrorDto("Invalid status"), 400);
                    return;
                }
                hql.append(" AND o.status = :status");
                hasCondition = true;
            }
            if (user != null && !user.isBlank()) {
                hql.append(hasCondition ? " OR c.phone LIKE :user" : " AND c.phone LIKE :user");
                hasCondition = true;
            }
            if (courier != null && !courier.isBlank()) {
                hql.append(hasCondition ? " OR o.courier.phone LIKE :courier" : " AND o.courier.phone LIKE :courier");
            }

            Query<Order> query = session.createQuery(hql.toString(), Order.class);
            query.setParameter("restaurantId", restaurantId);
            if (status != null && !status.isBlank()) query.setParameter("status", OrderStatus.valueOf(status));
            if (user != null && !user.isBlank()) query.setParameter("user", "%" + user + "%");
            if (courier != null && !courier.isBlank()) query.setParameter("courier", "%" + courier + "%");

            List<Order> orders = query.getResultList();
            List<ResponseOrder> result = orders.stream()
                    .map(order -> new ResponseOrder(
                            order.getId(), order.getDeliveryAddress(), order.getUser().getId(),
                            order.getVendor().getId(), order.getCoupon() != null ? order.getCoupon().getId() : null,
                            orderDao.getById(order.getId()).getItems(), order.getRawPrice(), order.getTaxFee(), order.getAdditionalFee(),
                            order.getCourierFee(), order.getPayPrice(), order.getCourierId(),
                            order.getStatus().toString(), order.getCreatedAt(), order.getUpdatedAt()
                    ))
                    .filter(responseOrder -> search == null || !search.isBlank() || !responseOrder.getItemIds().isEmpty())
                    .collect(Collectors.toList());

            sendResponse(exchange, result, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error: " + e.getMessage()), 500);
        }
    }

    public void handleUpdateOrderStatus(HttpExchange exchange, Long orderId) throws IOException {
        String phone = isAuthorizedVendor(exchange);
        if (phone == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User seller = userDao.getByPhone(phone);
            if (seller == null || seller.getRole() != Role.seller) {
                sendResponse(exchange, new ErrorDto("Unauthorized: user not found or not a seller"), 403);
                return;
            }

            Order order = orderDao.getById(orderId);
            if (order == null) {
                sendResponse(exchange, new ErrorDto("Order not found"), 404);
                return;
            }

            if (!order.getVendor().getVendor().getId().equals(seller.getId())) {
                sendResponse(exchange, new ErrorDto("Unauthorized: order not associated with this seller"), 403);
                return;
            }

            if (order.getVendor().getStatus() != ResStatus.approved) {
                sendResponse(exchange, new ErrorDto("Restaurant is not approved"), 403);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("status") || json.get("status").isJsonNull()) {
                sendResponse(exchange, new ErrorDto("Missing status field"), 400);
                return;
            }

            String newStatus = json.get("status").getAsString();
            if (!(order.getStatus().equals(OrderStatus.submitted) || order.getStatus().equals(OrderStatus.waitingVendor))) {
                sendResponse(exchange, new ErrorDto("Invalid status value"), 400);
                return;
            }
            List<String> validStatuses = List.of("accepted", "rejected", "served");

            if (!validStatuses.contains(newStatus)) {
                sendResponse(exchange, new ErrorDto("Invalid status value"), 400);
                return;
            }

            session.beginTransaction();
            switch (newStatus) {
                case "accepted" -> order.setStatus(OrderStatus.waitingVendor);
                case "rejected" -> order.setStatus(OrderStatus.cancelled);
                case "served" -> order.setStatus(OrderStatus.findingCourier);
            }
            order.setUpdatedAt(LocalDateTime.now());
            session.update(order);
            session.getTransaction().commit();

            JsonObject resp = new JsonObject();
            resp.addProperty("message", "Order status changed successfully");
            sendResponse(exchange, resp, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error: " + e.getMessage()), 500);
        }
    }
}
