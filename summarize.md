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