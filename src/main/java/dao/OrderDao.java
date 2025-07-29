package dao;

import entity.Order;
import entity.OrderStatus;
import jakarta.persistence.Query;
import org.hibernate.Session;
import util.HibernateUtil;

import java.util.List;

public class OrderDao {
    public Order getById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT o FROM Order o LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.foodItem LEFT JOIN FETCH o.user LEFT JOIN FETCH o.vendor WHERE o.id = :id", Order.class)
                    .setParameter("id", id)
                    .getSingleResult();
        }
    }

    public List<Order> getByCustomerId(Long customerId, String search, String vendor) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN FETCH o.items oi " +
                            "LEFT JOIN FETCH oi.foodItem f " +
                            "LEFT JOIN FETCH o.user " +
                            "LEFT JOIN FETCH o.vendor v " +
                            "WHERE o.user.id = :customerId"
            );
            if (search != null && !search.isBlank()) {
                hql.append(" AND LOWER(f.name) LIKE LOWER(:search)");
            }
            if (vendor != null && !vendor.isBlank()) {
                hql.append(" AND LOWER(v.name) LIKE LOWER(:vendor)");
            }
            Query query = session.createQuery(hql.toString(), Order.class);
            query.setParameter("customerId", customerId);
            if (search != null && !search.isBlank()) {
                query.setParameter("search", "%" + search + "%");
            }
            if (vendor != null && !vendor.isBlank()) {
                query.setParameter("vendor", "%" + vendor + "%");
            }
            return query.getResultList();
        }
    }

    public List<Order> getByVendorId(Long vendorId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT o FROM Order o LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.foodItem LEFT JOIN FETCH o.user LEFT JOIN FETCH o.vendor WHERE o.vendor.id = :vendorId", Order.class)
                    .setParameter("vendorId", vendorId)
                    .getResultList();
        }
    }

    public Order getByIdAndVendorId(Long orderId, Long vendorId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT o FROM Order o LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.foodItem LEFT JOIN FETCH o.user LEFT JOIN FETCH o.vendor WHERE o.id = :orderId AND o.vendor.id = :vendorId", Order.class)
                    .setParameter("orderId", orderId)
                    .setParameter("vendorId", vendorId)
                    .getSingleResult();
        }
    }

    public List<Order> getAvailableDeliveries() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT o FROM Order o " +
                                    "LEFT JOIN FETCH o.items oi " +
                                    "LEFT JOIN FETCH oi.foodItem " +
                                    "LEFT JOIN FETCH o.user " +
                                    "LEFT JOIN FETCH o.vendor " +
                                    "WHERE o.status = :status AND o.courierId IS NULL",
                            Order.class
                    ).setParameter("status", OrderStatus.findingCourier)
                    .getResultList();
        }
    }

    public List<Order> getDeliveryHistory(Long courierId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT o FROM Order o " +
                                    "LEFT JOIN FETCH o.items oi " +
                                    "LEFT JOIN FETCH oi.foodItem " +
                                    "LEFT JOIN FETCH o.user " +
                                    "LEFT JOIN FETCH o.vendor " +
                                    "WHERE o.courierId = :courierId",
                            Order.class
                    ).setParameter("courierId", courierId)
                    .getResultList();
        }
    }

    public void update(Order order) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.update(order);
            session.getTransaction().commit();
        }
    }

    public List<Order> getAll() {
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
}