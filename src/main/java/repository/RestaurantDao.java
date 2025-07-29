package repository;

import entity.ResStatus;
import entity.Restaurant;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.List;

public class RestaurantDao {
    public void save(Restaurant restaurant) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(restaurant);
            session.getTransaction().commit();
        }
    }

    public Restaurant getById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Restaurant restaurant = session.createQuery(
                    "FROM Restaurant r LEFT JOIN FETCH r.vendor WHERE r.id = :id",
                    Restaurant.class
            ).setParameter("id", id).uniqueResult();
            if (restaurant != null) {
                if (restaurant.getTaxFee() == null) {
                    restaurant.setTaxFee(0);
                }
                if (restaurant.getAdditionalFee() == null) {
                    restaurant.setAdditionalFee(0);
                }
            }
            return restaurant;
        }
    }

    public List<Restaurant> getByVendorPhone(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Restaurant> query = session.createQuery(
                    "FROM Restaurant r WHERE r..phone = :phone",
                    Restaurant.class
            );
            query.setParameter("phone", phone);
            return query.list();
        }
    }

    public List<Restaurant> getApprovedRestaurants() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Restaurant> query = session.createQuery(
                    "FROM Restaurant r WHERE r.status = :status",
                    Restaurant.class
            );
            query.setParameter("status", ResStatus.approved);
            return query.list();
        }
    }

    public List<Restaurant> getPendingRestaurants() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Restaurant> query = session.createQuery(
                    "FROM Restaurant r WHERE r.status = :status",
                    Restaurant.class
            );
            query.setParameter("status", ResStatus.pending);
            return query.list();
        }
    }

    public boolean isPhoneTaken(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(*) FROM Restaurant r WHERE r.phone = :phone",
                    Long.class
            );
            query.setParameter("phone", phone);
            return query.uniqueResult() > 0;
        }
    }
}