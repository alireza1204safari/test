package dao;

import entity.Menu;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.List;

public class MenuDao {
    public void save(Menu menu) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(menu);
            session.getTransaction().commit();
        }
    }

    public void delete(Menu menu) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.delete(menu);
            session.getTransaction().commit();
        }
    }

    public Menu getByTitleAndRestaurantId(String title, Long restaurantId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Menu> query = session.createQuery(
                    "FROM Menu m LEFT JOIN FETCH m.items WHERE m.title = :title AND m.restaurant.id = :restaurantId",
                    Menu.class
            );
            query.setParameter("title", title);
            query.setParameter("restaurantId", restaurantId);
            return query.uniqueResult();
        }
    }

    public List<Menu> getByRestaurantId(Long restaurantId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Menu> query = session.createQuery(
                    "FROM Menu m LEFT JOIN FETCH m.items WHERE m.restaurant.id = :restaurantId",
                    Menu.class
            );
            query.setParameter("restaurantId", restaurantId);
            return query.list();
        }
    }
}