package repository;

import entity.FoodItem;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

public class FoodItemDao {
    public void save(FoodItem foodItem) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(foodItem);
            session.getTransaction().commit();
        }
    }

    public void delete(FoodItem foodItem) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.delete(foodItem);
            session.getTransaction().commit();
        }
    }

    public FoodItem getById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<FoodItem> query = session.createQuery(
                    "FROM FoodItem fi LEFT JOIN FETCH fi.keywords WHERE fi.id = :id",
                    FoodItem.class
            );
            query.setParameter("id", id);
            return query.uniqueResult();
        }
    }
}