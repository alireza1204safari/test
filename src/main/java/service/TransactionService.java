package service;

import com.sun.net.httpserver.HttpExchange;
import dao.OrderDao;
import dao.TransactionDao;
import dao.UserDao;
import entity.Order;
import entity.OrderStatus;
import entity.Transaction;
import entity.TransactionMethod;
import entity.TransactionStatus;
import entity.User;
import dto.ErrorDto;
import dto.ResponseMessage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TransactionService extends BaseService {
    private final TransactionDao transactionDao = new TransactionDao();
    private final UserDao userDao = new UserDao();
    private final OrderDao orderDao = new OrderDao();

    public TransactionService() {
        super();
    }

    public void getTransactions(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            phone = isAuthorizedCourier(exchange);
            if (phone == null) {
                return;
            }
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("transactions_list", transactionDao.getByUserId(user.getId()));
        sendResponse(exchange, response, 200);
    }

    public void topUpWallet(HttpExchange exchange, Map<String, Object> requestBody) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            return;
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Double amount = (Double) requestBody.get("amount");
        if (amount == null || amount <= 0) {
            sendResponse(exchange, new ErrorDto("Invalid amount"), 400);
            return;
        }
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setMethod(TransactionMethod.WALLET);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionDao.save(transaction);
        sendResponse(exchange, new ResponseMessage("Wallet topped up successfully"), 200);
    }

    public void onlinePayment(HttpExchange exchange, Map<String, Object> requestBody) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) {
            return;
        }
        User user = userDao.getByPhone(phone);
        if (user == null) {
            sendResponse(exchange, new ErrorDto("User not found"), 404);
            return;
        }
        Long orderId = Long.parseLong((String) requestBody.get("order_id"));
        Order order = orderDao.getById(orderId);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            sendResponse(exchange, new ErrorDto("Order not found or not owned by user"), 404);
            return;
        }
        if (order.getStatus() != OrderStatus.submitted) {
            sendResponse(exchange, new ErrorDto("Order cannot be paid at this stage"), 400);
            return;
        }
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setOrder(order);
        transaction.setMethod(TransactionMethod.ONLINE);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionDao.save(transaction);
        order.setStatus(OrderStatus.waitingVendor);
        order.setUpdatedAt(new Date());
        orderDao.update(order);
        sendResponse(exchange, new ResponseMessage("Payment processed successfully"), 200);
    }
}