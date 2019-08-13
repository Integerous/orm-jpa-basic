package relationalMapping;

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

            Team team = new Team();
            team.setName("TeamA");
            entityManager.persist(team);

            Member member = new Member();
            member.setUsername("member1");
//            member.setTeamId(team.getId()); // 외래키의 식별자를 직접 다루고 있다. team을 persist하면 id가 들어가고나서 영속상태가 되니까 id를 바로 get 할 수 있다.
            member.setTeam(team); // 단방향 연관관계 설정, 참조 저장. team을 넣으면 JPA가 알아서 team에서 PK값을 꺼내서 INSERT 할 때 FK값으로 사용한다.
            entityManager.persist(member);

            Member findMember = entityManager.find(Member.class, member.getId());
//            Long findTeamId = findMember.getTeamId();
//            Team findTeam = entityManager.find(Team.class, findTeamId);
            // 연관관계가 없기 때문에 위와 같이 DB에서 계속 꺼내와야 한다. (객체지향적인 방식이 아니다.)
            // 하지만 아래와 같이 연관관계를 사용하면 member에서 바로 team을 가져올 수 있다.
            Team findTeam = member.getTeam();

            // 새로운 팀B
            Team teamB = new Team();
            teamB.setName("TeamB");
            entityManager.persist(teamB);

            // 회원1에 새로운 팀B 설정
            member.setTeam(teamB);

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
