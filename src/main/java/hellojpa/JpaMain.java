package hellojpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.List;

public class JpaMain {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");
        EntityManager entityManager = emf.createEntityManager();
//----------------------------------
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        try {
            // 삽입
            Member member = new Member();
            member.setId(1L);
            member.setName("HelloA");
            entityManager.persist(member);

            // 조회
            Member findMember = entityManager.find(Member.class, 1L);
            System.out.println("findMember.id = " + findMember.getId());
            System.out.println("findMember.name = " + findMember.getName());

            // 삭제
            entityManager.remove(findMember);

            // 수정
            findMember.setName("changedName");

            // JPQL
            List<Member> results = entityManager.createQuery("select m from Member as m", Member.class)
                    .setFirstResult(1)
                    .setMaxResults(5) // 이렇게 사용하면 간편하게 페이징 처리가 된다. (방언에 따라 페이징 쿼리가 나간다)
                    .getResultList(); // JPA는 절대 테이블을 대상으로 코드를 짜지 않는다. 테이블이 아니라 객체가 대상이 된다.

            for (Member result : results) {
                System.out.println("member.name = " + result.getName());
            }

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
        } finally {
            entityManager.close();
        }
//----------------------------------
        entityManager.close();
        emf.close();
    }
}
