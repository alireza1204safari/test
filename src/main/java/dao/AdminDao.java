package dao;

import entity.Coupon;
import entity.Order;
import entity.Transaction;
import entity.User;
import org.hibernate.Session;
import util.HibernateUtil;

import java.util.List;

public class AdminDao {
    public List<User> getAllUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT u FROM User u", User.class)
                    .getResultList();
        }
    }

    public User getUserById(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT u FROM User u WHERE u.id = :userId", User.class)
                    .setParameter("userId", userId)
                    .getSingleResultOrNull();
        }
    }

    public void updateUser(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.update(user);
            session.getTransaction().commit();
        }
    }

    public List<Order> getAllOrders() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT o FROM Order o " +
                            "LEFT JOIN FETCH o.items oi " +
                            "LEFT JOIN FETCH oi.foodItem " +
                            "LEFT JOIN FETCH o.user " +
                            "LEFT JOIN FETCH o.vendor",
                    Order.class
            ).getResultList();
        }
    }

    public List<Transaction> getAllTransactions() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT t FROM Transaction t " +
                            "LEFT JOIN FETCH t.order o " +
                            "LEFT JOIN FETCH o.items oi " +
                            "LEFT JOIN FETCH oi.foodItem " +
                            "LEFT JOIN FETCH t.user",
                    Transaction.class
            ).getResultList();
        }
    }

    public void saveCoupon(Coupon coupon) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(coupon);
            session.getTransaction().commit();
        }
    }

    public List<Coupon> getAllCoupons() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT c FROM Coupon c", Coupon.class)
                    .getResultList();
        }
    }

    public Coupon getCouponById(Long couponId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT c FROM Coupon c WHERE c.id = :couponId", Coupon.class)
                    .setParameter("couponId", couponId)
                    .getSingleResultOrNull();
        }
    }

    public Coupon getCouponByCode(String couponCode) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT c FROM Coupon c WHERE c.couponCode = :couponCode", Coupon.class)
                    .setParameter("couponCode", couponCode)
                    .getSingleResultOrNull();
        }
    }

    public void deleteCoupon(Coupon coupon) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.delete(coupon);
            session.getTransaction().commit();
        }
    }
}