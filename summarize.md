## JPA 구동 방식

- JPA는 Persistence라는 클래스가 META-INF/persistence.xml 을 읽어서  
EntityManagerFactory 라는 클래스를 생성한다.
- 그리고 EntityManagerFactory에서 EntityManager들을 생성한다.
- EntityManagerFactory는 어플리케이션 로딩 시점에 딱 하나만 만들어야 한다.
- 그리고 트랜잭션 단위 마다 EntityManager를 생성해서 사용한다.
- EntityManager는 쓰레드 간에 공유하면 안되고 사용하고 버려야 한다.

- JPA의 모든 데이터 변경은 트랜잭션 안에서 실행한다.
- RDB는 데이터 변경을 트랜잭션안에서 실행되도록 다 설계되어 있다. 때문에 트랜잭션을 걸지 않아도 DB가 트랜잭션 개념을 가지고 있기 때문에 트랜잭션 안에서 데이터가 변경된다.


## JPQL

- JPQL은 SQL을 추상화한 객체지향 쿼리이다. (한마디로 JPQL = 객체지향 SQL)
  - JPQL은 Entity 객체를 대상으로 쿼리하고,
  - SQL은 DB 테이블을 대상으로 쿼리한다.
- 검색을 할 때에도 테이블이 아닌 Entity 객체를 대상으로 검색한다.
- 모든 DB 데이터를 객체로 변환해서 검색하는 것은 불가능하다.
- 어플리케이션이 필요한 데이터만 DB에서 불러오려면 결국 검색 조건이 포함된 SQL이 필요하다.
  - DB의 테이블을 대상으로 쿼리를 날리면 해당 DB에 종속적인 설계가 된다.
  - 때문에 Entity 객체를 대상으로 쿼리를 할 수 있는 JPQL이 제공된 것이다.
- 방언을 바꿔도 JPQL을 바꿀 필요가 없다. (특정 데이터베이스 SQL에 의존하지 않는다.)


## 영속성 컨텍스트 (PersistenceContext)

***JPA에서 가장 중요한 2가지***
- 객체와 관계형 데이터베이스 매핑하기
- 영속성 컨텍스트

***영속성 컨텍스트 (PersistenceContext)***
- 엔티티를 영구 저장하는 환경이라는 뜻
- `EntityManager.persist(entity);`
  - 이것은 DB에 저장한다는 것이 아니라, 엔티티를 영속성 컨텍스트에 저장한다는 것이다.
- 영속성 컨텍스트는 논리적인 개념
- EntityManager를 통해서 영속성 컨텍스트에 접근한다.
  - EntityManager 안에 눈에 보이지 않는 PersistenceContext 공간이 생긴다고 이해하면 된다.
  
***엔티티의 생명 주기***
- 비영속(new/transient)
  - 영속성 컨텍스트와 전혀 관계가 없는 새로운 상태
- 영속(managed)
  - 영속성 컨텍스트에 관리되는 상태
- 준영속(detached)
  - 영속성 컨텍스트에 저장되었다가 분리된 상태
- 삭제(removed)
  - 삭제된 상태
  
***비영속***
~~~java
//객체를 생성한 상태(비영속)
Member member = new Membeer();
member.setId("member1");
member.setUsername("회원1");
~~~

***영속***
~~~java
//객체를 생성한 상태(비영속)
Member member = new Membeer();
member.setId("member1");
member.setUsername("회원1");

EntityManager em = emf.createEntityManager();
em.getTransaction().begin();

//객체를 저장한 상태(영속)
em.persist(member);
~~~

그런데 `em.persist(member)`를 해서 영속상태가 된다고 DB에 저장되는 것이 아니다.
트랜잭션을 커밋하는 시점에 DB에 쿼리가 날라간다.

***준영속***
~~~java
...

em.detach(member); //회원 엔티티를 영속성 컨텍스트에서 분리 (준영속 상태)
~~~

***삭제***
~~~java
...
em.remove(member); // 객체를 삭제한 상태
~~~

## 영속성 컨텍스트의 이점
1. 1차 캐시
2. 동일성(identity) 보장
3. 트랜잭션을 지원하는 쓰기 지연 (transactional write-behind)
4. 변경 감지(Dirty Checking)
5. 지연 로딩(Lazy Loading)

***1. 1차 캐시***
영속성 컨텍스트는 내부에 1차 캐시를 들고있다.  
사실 1차캐시를 영속성 컨텍스트로 이해해도 된다.

~~~java
...

//1차 캐시에 저장됌
em.persist(member);

//1차 캐시에서 조회
Member findMember = em.find(Member.class, "member1");
~~~

위의 코드에서 처럼 em.find()로 조회를 하게 되면,  
DB를 뒤지는 것이 아니라, 1차 캐시에서 먼저 조회한다.

그런데, 예를 들어 member2가 DB에는 있고 1차 캐시에는 없는 경우에는  
1차 캐시에서 먼저 찾아보고, 없으면 DB에서 조회한 후에  
DB에서 조회한 Entity를 1차 캐시에 저장한다.  
그리고 반환할 때에는 1차캐시에서 반환한다.

하지만 1차캐시가 엄청난 이득이 있는 것은 아니다.  
왜냐하면 EntityManager는 DB 트랜잭션 단위로 만들고, DB 트랜잭션이 끝날 때 같이 종료시킨다.
때문에 EntityManager가 종료되면 1차캐시도 같이 사라지기 때문에 찰나의 순간에만 이득이 있다.

어플리케이션 전체에서 사용하는 캐시를 JPA에서는 2차 캐시라고 하고,  
DB 트랜잭션 안에서만 사용하는 캐시를 1차 캐시라고 한다.

***2. 영속 Entity의 동일성 보장***
~~~java
Member a = em.find(Member.class, "member1");
Member b = em.find(Member.class, "member1");

System.out.println(a == b); // 동일성 비교 true
~~~

1차 캐시로 반복가능한 읽기(Repeatable read) 등급의 트랜잭션 격리 수준을  
데이터베이스가 아닌 애플리케이션 차원에서 제공한다.


***3. 트랜잭션을 지원하는 쓰기 지연***
~~~java
EntityManager em = emf.createEntityManager();
EntityTransaction transaction = em.getTransaction();

//EntityManager는 데이터 변경 시 트랜잭션을 시작해야 한다.
transaction.begin();

em.persist(memberA);
em.persist(memberB);
// 여기까지도 Insert SQL을 DB에 보내지 않는다.

// 그러다가 커밋하는 순간 DB에 Insert SQL을 보낸다.
transaction.commit();
~~~

영속성 컨텍스트 안에는 1차 캐시와 더불어 `쓰기 지연 SQL 저장소`가 있다.  
JPA는 1차캐시에 객체를 저장하면서 이 객체를 분석해서 Insert SQL을 생성하고,  
쓰기 지연 SQL 저장소에 쌓아둔다.

쌓아둔 SQL은 `transaction.commit()` 시점에 flush되면서 쿼리가 날라가고 커밋된다.

이 때, `<property name="hibernate.jdbc.batch_size" value="10"/>` 옵션을 persistence.xml에 추가하면
사이즈(10)만큼 모아서 DB에 쿼리를 날리고 DB를 커밋한다.


***4. 변경 감지 (Dirty Checking)***
~~~java
EntityManager em = emf.createEntityManager();
EntityTransaction transaction = em.getTransaction();
transaction.begin();

// 영속 엔티티 조회
Member memberA = em.find(Member.class, "memberA");

// 영속 엔티티 데이터 수정
memberA.setUsername("hi");
memberA.setAge(10);

// em.update(member) 이런 코드가 있어야 하지 않을까?
// 필요없다.

transaction.commit();
~~~

1차캐시에는 Id와 Entity, 스냅샷이 존재한다.  
스냅샷은 값을 읽어온 시점의 상태를 저장한 것이다.  

JPA는 DB 트랜잭션을 커밋하는 시점에 내부적으로 flush()가 호출되는데,  
이 때, Entity와 스냅샷을 비교한다.  
비교 결과 바뀐 것이 있으면 Update 쿼리를 `쓰기 지연 SQL 저장소`에 저장해 둔다.

그 이후에 flush를 하게 되면, 저장해둔 Update 쿼리를 날린 후에 커밋을 한다.