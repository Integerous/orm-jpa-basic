package jpabook.jpashop;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

public class JpaMain {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");
        EntityManager entityManager = emf.createEntityManager();
//----------------------------------
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        try {

            transaction.commit();

        } catch (Exception e) {
            transaction.rollback();
        } finally {
            entityManager.close();
        }
//----------------------------------
        emf.close();
    }
}
