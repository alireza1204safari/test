package dao;

import entity.Rating;
import org.hibernate.Session;
import util.HibernateUtil;

import java.util.List;

public class RatingDao {
    public void save(Rating rating) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(rating);
            session.getTransaction().commit();
        }
    }

    public List<Rating> getByUserId(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT r FROM Rating r LEFT JOIN FETCH r.order o LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.foodItem WHERE r.user.id = :userId",
                    Rating.class
            ).setParameter("userId", userId).getResultList();
        }
    }

    public Rating getById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT r FROM Rating r LEFT JOIN FETCH r.order o LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.foodItem WHERE r.id = :id",
                    Rating.class
            ).setParameter("id", id).getSingleResultOrNull();
        }
    }

    public List<Rating> getByItemId(Long itemId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT r FROM Rating r LEFT JOIN FETCH r.order o LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.foodItem fi WHERE fi.id = :itemId",
                    Rating.class
            ).setParameter("itemId", itemId).getResultList();
        }
    }

    public void delete(Rating rating) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.delete(rating);
            session.getTransaction().commit();
        }
    }
}