package repository;

import entity.User;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

public class UserDao {
    public void save(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(user);
            session.getTransaction().commit();
        }
    }

    public User getByPhone(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User u WHERE u.phone = :phone",
                    User.class
            );
            query.setParameter("phone", phone);
            return query.uniqueResult();
        }
    }

    public User getById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(User.class, id);
        }
    }

    public boolean isPhoneTaken(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM User u WHERE u.phone = :phone", Long.class);
            query.setParameter("phone", phone);
            return query.uniqueResult() > 0;
        }
    }
}