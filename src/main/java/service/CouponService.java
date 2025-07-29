package service;

import com.sun.net.httpserver.HttpExchange;
import dto.ErrorDto;
import dto.ResponseCoupon;
import entity.Coupon;
import org.hibernate.Session;
import repository.CouponDao;
import util.HibernateUtil;

import java.io.IOException;
import java.time.LocalDate;

public class CouponService extends BaseService {
    private final CouponDao couponDao;

    public CouponService() {
        super();
        this.couponDao = new CouponDao();
    }

    public void handleCheckCoupon(HttpExchange exchange) throws IOException {
        String phone = isAuthorizedBuyer(exchange);
        if (phone == null) return;

        String couponCode = exchange.getRequestURI().getQuery();
        if (couponCode == null || !couponCode.startsWith("coupon_code=")) {
            sendResponse(exchange, new ErrorDto("Invalid input: coupon_code is required"), 400);
            return;
        }

        couponCode = couponCode.replace("coupon_code=", "");
        if (couponCode.isBlank()) {
            sendResponse(exchange, new ErrorDto("Invalid input: coupon_code cannot be empty"), 400);
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Coupon coupon = couponDao.getByCouponCode(couponCode);
            if (coupon == null) {
                sendResponse(exchange, new ErrorDto("Coupon not found"), 404);
                return;
            }

            // Validate coupon
            LocalDate today = LocalDate.now();
            if (coupon.getUserCount() <= 0) {
                sendResponse(exchange, new ErrorDto("Coupon is no longer valid (user count exhausted)"), 400);
                return;
            }
            if (coupon.getStartDate().isAfter(today)) {
                sendResponse(exchange, new ErrorDto("Coupon is not yet valid"), 400);
                return;
            }
            if (coupon.getEndDate().isBefore(today)) {
                sendResponse(exchange, new ErrorDto("Coupon has expired"), 400);
                return;
            }
            if (!coupon.getType().equals("fixed") && !coupon.getType().equals("percent")) {
                sendResponse(exchange, new ErrorDto("Invalid coupon type"), 400);
                return;
            }

            ResponseCoupon response = new ResponseCoupon(
                    coupon.getId(),
                    coupon.getCouponCode(),
                    coupon.getType(),
                    coupon.getValue(),
                    coupon.getMinPrice(),
                    coupon.getUserCount(),
                    coupon.getStartDate(),
                    coupon.getEndDate()
            );

            sendResponse(exchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, new ErrorDto("Internal server error"), 500);
        }
    }
}