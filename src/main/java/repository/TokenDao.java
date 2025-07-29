package repository;

import entity.Token;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

public class TokenDao {

    public void save(Token token) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(token);
            transaction.commit();
        }
    }

    public boolean isTokenInvalidated(String token) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Token existingToken = session.createQuery("from Token where token = :token", Token.class)
                    .setParameter("token", token)
                    .uniqueResult();
            return existingToken != null;
        }
    }
}