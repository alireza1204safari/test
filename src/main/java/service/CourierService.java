package service;

import com.sun.net.httpserver.HttpExchange;
import dao.OrderDao;
import dao.UserDao;
import entity.Order;
import entity.OrderStatus;
import entity.User;
import dto.ErrorDto;
import dto.ResponseMessage;
import entity.UserStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CourierService extends BaseService {
    private final OrderDao orderDao = new OrderDao();
    private final UserDao userDao = new UserDao();

    public CourierService() {
        super();
    }

    public void getAvailableDeliveries(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedCourier(exchange);
        if (phone == null) {
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("orders_list", orderDao.getAvailableDeliveries());
        sendResponse(exchange, response, 200);
    }

    public void updateDeliveryStatus(HttpExchange exchange, Long orderId, Map<String, String> requestBody) throws IOException {
        String phone = isAuthorizedCourier(exchange);
        if (phone == null) {
            return;
        }
        User courier = userDao.getByPhone(phone);
        if (courier == null) {
            sendResponse(exchange, new ErrorDto("Courier not found"), 404);
            return;
        }
        Order order = orderDao.getById(orderId);
        if (order == null) {
            sendResponse(exchange, new ErrorDto("Order not found"), 404);
            return;
        }
        String newStatusStr = requestBody.get("status");
        if (!(newStatusStr.equals("accepted") || newStatusStr.equals("rejected") || newStatusStr.equals("delivered"))) {
            throw new IllegalArgumentException("Invalid status value");
        }
        OrderStatus orderStatus = newStatusStr.equals("accepted") ? OrderStatus.onTheWay : newStatusStr.equals("delivered") ? OrderStatus.completed : OrderStatus.cancelled;

        // بررسی انتقال وضعیت مجاز
        if (!isValidStatusTransition(order.getStatus(), orderStatus, order.getCourierId(), courier.getId())) {
            sendResponse(exchange, new ErrorDto("Invalid status transition or courier not assigned"), 400);
            return;
        }
        // تخصیص پیک در صورت پذیرش سفارش
        if (order.getStatus() == OrderStatus.findingCourier && order.getCourierId() == null) {
            order.setCourierId(courier.getId());
        }
        order.setStatus(orderStatus);
        order.setUpdatedAt(new Date());
        orderDao.update(order);
        sendResponse(exchange, new ResponseMessage("Order status updated successfully"), 200);
    }

    public void getDeliveryHistory(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedCourier(exchange);
        if (phone == null) {
            return;
        }
        User courier = userDao.getByPhone(phone);
        if (courier == null) {
            sendResponse(exchange, new ErrorDto("Courier not found"), 404);
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("orders_list", orderDao.getDeliveryHistory(courier.getId()));
        sendResponse(exchange, response, 200);
    }

    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus, Long currentCourierId, Long courierId) {
        return switch (currentStatus) {
            case findingCourier -> newStatus == OrderStatus.onTheWay;
            case onTheWay -> newStatus == OrderStatus.completed && (currentCourierId != null && currentCourierId.equals(courierId));
            default -> false;
        };
    }
}