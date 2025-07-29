package repository;

import entity.Order;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.List;

public class OrderDao {


    public Order getById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Order> query = session.createQuery(
                    "FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.user LEFT JOIN FETCH o.vendor WHERE o.id = :id",
                    Order.class
            );
            query.setParameter("id", id);
            return query.uniqueResult();
        }
    }

    public List<Order> getByCustomerId(Long customerId, String search, String restaurantName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("""
                SELECT DISTINCT o
                FROM Order o
                LEFT JOIN FETCH o.items oi
                LEFT JOIN FETCH oi.foodItem f
                LEFT JOIN FETCH o.user
                LEFT JOIN FETCH o.vendor v
                WHERE o.user.id = :customerId
            """);

            boolean hasCondition = false;
            if (search != null && !search.isBlank()) {
                hql.append(" AND LOWER(f.name) LIKE LOWER(:search) ");
                hasCondition = true;
            }
            if (restaurantName != null && !restaurantName.isBlank()) {
                hql.append(hasCondition ? " OR LOWER(v.name) LIKE LOWER(:restaurantName) " : " AND LOWER(v.name) LIKE LOWER(:restaurantName) ");
            }

            Query<Order> query = session.createQuery(hql.toString(), Order.class);
            query.setParameter("customerId", customerId);

            if (search != null && !search.isBlank()) {
                query.setParameter("search", "%" + search + "%");
            }

            if (restaurantName != null && !restaurantName.isBlank()) {
                query.setParameter("restaurantName", "%" + restaurantName + "%");
            }

            return query.list();
        }
    }
    public List<Order> getByVendorId(Long vendorId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Order> query = session.createQuery(
                    "FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.user LEFT JOIN FETCH o.vendor WHERE o.vendor.id = :vendorId",
                    Order.class
            );
            query.setParameter("vendorId", vendorId);
            return query.list();
        }
    }

    public Order getByIdAndVendorId(Long orderId, Long vendorId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Order> query = session.createQuery(
                    "FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.user LEFT JOIN FETCH o.vendor WHERE o.id = :orderId",
                    Order.class
            );
            query.setParameter("orderId", orderId);
            return query.uniqueResult();
        }
    }


}