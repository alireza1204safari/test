package dao;

import entity.Coupon;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

public class CouponDao {
    public Coupon getByCouponCode(String couponCode) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Coupon> query = session.createQuery(
                    "FROM Coupon c WHERE c.couponCode = :couponCode",
                    Coupon.class
            );
            query.setParameter("couponCode", couponCode);
            return query.uniqueResult();
        }
    }
}