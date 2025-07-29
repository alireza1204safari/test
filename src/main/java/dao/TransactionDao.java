package dao;

import entity.Transaction;
import org.hibernate.Session;
import util.HibernateUtil;

import java.util.List;

public class TransactionDao {
    public void save(Transaction transaction) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(transaction);
            session.getTransaction().commit();
        }
    }

    public List<Transaction> getByUserId(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT t FROM Transaction t " +
                            "LEFT JOIN FETCH t.order o " +
                            "LEFT JOIN FETCH o.items oi " +
                            "LEFT JOIN FETCH oi.foodItem " +
                            "LEFT JOIN FETCH t.user " +
                            "WHERE t.user.id = :userId",
                    Transaction.class
            ).setParameter("userId", userId).getResultList();
        }
    }
}