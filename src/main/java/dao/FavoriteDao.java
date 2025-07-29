package dao;

import entity.Favorite;
import entity.FoodItem;
import entity.User;
import org.hibernate.Session;
import util.HibernateUtil;

import java.util.List;

public class FavoriteDao {
    public void save(Favorite favorite) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(favorite);
            session.getTransaction().commit();
        }
    }

    public List<FoodItem> getFavoritesByUserId(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT f.foodItem FROM Favorite f WHERE f.user.id = :userId",
                    FoodItem.class
            ).setParameter("userId", userId).getResultList();
        }
    }

    public Favorite getByUserIdAndItemId(Long userId, Long itemId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT f FROM Favorite f WHERE f.user.id = :userId AND f.foodItem.id = :itemId",
                            Favorite.class
                    ).setParameter("userId", userId)
                    .setParameter("itemId", itemId)
                    .getSingleResultOrNull();
        }
    }

    public void delete(Favorite favorite) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.delete(favorite);
            session.getTransaction().commit();
        }
    }
}