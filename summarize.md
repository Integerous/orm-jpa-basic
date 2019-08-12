# JPA 구동 방식

- JPA는 Persistence라는 클래스가 META-INF/persistence.xml 을 읽어서  
EntityManagerFactory 라는 클래스를 생성한다.
- 그리고 EntityManagerFactory에서 EntityManager들을 생성한다.
- EntityManagerFactory는 어플리케이션 로딩 시점에 딱 하나만 만들어야 한다.
- 그리고 트랜잭션 단위 마다 EntityManager를 생성해서 사용한다.
- EntityManager는 쓰레드 간에 공유하면 안되고 사용하고 버려야 한다.

- JPA의 모든 데이터 변경은 트랜잭션 안에서 실행한다.
- RDB는 데이터 변경을 트랜잭션안에서 실행되도록 다 설계되어 있다. 때문에 트랜잭션을 걸지 않아도 DB가 트랜잭션 개념을 가지고 있기 때문에 트랜잭션 안에서 데이터가 변경된다.


# JPQL

- JPQL은 SQL을 추상화한 객체지향 쿼리이다. (한마디로 JPQL = 객체지향 SQL)
  - JPQL은 Entity 객체를 대상으로 쿼리하고,
  - SQL은 DB 테이블을 대상으로 쿼리한다.
- 검색을 할 때에도 테이블이 아닌 Entity 객체를 대상으로 검색한다.
- 모든 DB 데이터를 객체로 변환해서 검색하는 것은 불가능하다.
- 어플리케이션이 필요한 데이터만 DB에서 불러오려면 결국 검색 조건이 포함된 SQL이 필요하다.
  - DB의 테이블을 대상으로 쿼리를 날리면 해당 DB에 종속적인 설계가 된다.
  - 때문에 Entity 객체를 대상으로 쿼리를 할 수 있는 JPQL이 제공된 것이다.
- 방언을 바꿔도 JPQL을 바꿀 필요가 없다. (특정 데이터베이스 SQL에 의존하지 않는다.)


# 영속성 컨텍스트 (PersistenceContext)

### JPA에서 가장 중요한 2가지
- 객체와 관계형 데이터베이스 매핑하기
- 영속성 컨텍스트

### 영속성 컨텍스트 (PersistenceContext)
- 엔티티를 영구 저장하는 환경이라는 뜻
- `EntityManager.persist(entity);`
  - 이것은 DB에 저장한다는 것이 아니라, 엔티티를 영속성 컨텍스트에 저장한다는 것이다.
- 영속성 컨텍스트는 논리적인 개념
- EntityManager를 통해서 영속성 컨텍스트에 접근한다.
  - EntityManager 안에 눈에 보이지 않는 PersistenceContext 공간이 생긴다고 이해하면 된다.
  
### 엔티티의 생명 주기
- 비영속(new/transient)
  - 영속성 컨텍스트와 전혀 관계가 없는 새로운 상태
- 영속(managed)
  - 영속성 컨텍스트에 관리되는 상태
- 준영속(detached)
  - 영속성 컨텍스트에 저장되었다가 분리된 상태
- 삭제(removed)
  - 삭제된 상태
  
### 비영속
~~~java
//객체를 생성한 상태(비영속)
Member member = new Membeer();
member.setId("member1");
member.setUsername("회원1");
~~~

### 영속
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

### 준영속
~~~java
...

em.detach(member); //회원 엔티티를 영속성 컨텍스트에서 분리 (준영속 상태)
~~~

### 삭제
~~~java
...
em.remove(member); // 객체를 삭제한 상태
~~~

# 영속성 컨텍스트의 이점
1. 1차 캐시
2. 동일성(identity) 보장
3. 트랜잭션을 지원하는 쓰기 지연 (transactional write-behind)
4. 변경 감지(Dirty Checking)
5. 지연 로딩(Lazy Loading)

### 1. 1차 캐시
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

### 2. 영속 Entity의 동일성 보장
~~~java
Member a = em.find(Member.class, "member1");
Member b = em.find(Member.class, "member1");

System.out.println(a == b); // 동일성 비교 true
~~~

1차 캐시로 반복가능한 읽기(Repeatable read) 등급의 트랜잭션 격리 수준을  
데이터베이스가 아닌 애플리케이션 차원에서 제공한다.


### 3. 트랜잭션을 지원하는 쓰기 지연
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


### 4. 변경 감지 (Dirty Checking)
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


# 플러시 (Flush)
플러시는 영속성 컨텍스트의 변경내용을 DB에 반영(동기화)하는 것이다.  
다시 말해, 영속성 컨텍스트에 저장된 쿼리들을 DB에 날려주는 것이다.

### 플러시하는 방법
1. `em.flush()` 직접 호출
2. `트랜잭션 커밋` 자동 호출
3. `JPQL 쿼리 실행` 자동 호출


### JPQL 쿼리 실행시 플러시가 자동으로 호출되는 이유
~~~java
em.persist(memberA);
em.persist(memberB);
em.persist(memberC);

//중간에 JPQL 실행
query = em.createQuery("select m from Member m", Member.class);
List<Member> members = query.getResultList();
~~~

위의 코드에서 member들이 DB에 저장된 것은 아니기 때문에  
select 쿼리로 조회해봐도 나오지 않아야 하는 것이 정상이다.  
하지만 JPQL은 쿼리를 실행할 때 무조건 flush를 날려버리기 때문에 DB에 저장이 되고,  
select 쿼리로 조회가 가능한 것이다.


### 플러시 모드 옵션
`em.setFlushMode(FlushModeType.COMMIT)`
- FlushModeType.AUTO
  - 커밋이나 쿼리를 실행할 때 플러시 (기본값)
- FlushModeType.COMMIT
  - 커밋할 때만 플러시
  

# 준영속 상태
영속상태를 만드는 방법은 2가지다.  
- `em.persist()`를 하거나,  
- 비영속상태에서 `em.find()`로 조회하는 과정에서 1차캐시에 저장되면서 영속상태가 되는 것

준영속 상태는 영속 상태의 엔티티가 영속성 컨텍스트에서 분리(detached)되는 것이다.

### 준영속 상태로 만드는 방법
1. `em.detach(entity)`
  - 특정 엔티티만 준영속 상태로 전환
2. `em.clear()`
  - 영속성 컨텍스트를 완전히 초기화
3. `em.close()`
  - 영속성 컨텍스트를 종료


# 데이터베이스 스키마 자동 생성
- JPA는 어플리케이션 로딩 시점에 DB 테이블을 생성하는 기능을 제공한다. (운영에서는 쓰면 안되고 개발 환경에서 편하다)
- DB 방언을 활용해서 DB에 적절한 DDL을 생성한다.  
- 생성된 DDL은 운영환경에서는 사용하지 않고 적절하게 다듬은 후에 사용해야 한다.

### hibernate.hbm2ddl.auto
- create : 기존 테이블 삭제 후 다시 생성 (DROP + CREATE)
- create-drop : create와 같으나 종료 시점에 테이블 DROP
- update : 변경된 내용만 반영(운영DB에는 사용 금지, 삭제는 반영 안됌)
- validate : 엔티티와 테이블이 정상 매핑되었는지만 확인
- none : 사용하지 않음

### 주의사항
- **운영장비에는 절대 create, create-drop, update를 사용하면 안된다.**
- 개발 초기 단계는 create / update
- 테스트 서버는 update / validate (가급적 테스트 서버에도 쓰지 말라)
- 스테이징과 운영 서버는 vaildate / none

### DDL 생성 기능
- 제약조건 추가
  - 회원 이름은 필수, 10자 초과X => `@Column(nullable = false, length = 10)`
- 유니크 제약조건 추가
  - `@Table(uniqueConstraints = {@UniqueConstraint( name = "NAME_AGE_UNIQUE", columnNames = {"NAME", "AGE"} )})`
- DDL 생성 기능은 DDL을 자동 생성할 때만 사용되고,  JPA의 실행 로직에는 영향을 주지 않는다.


# 엔티티 매핑

### @Entity
- @Entity가 붙은 클래스는 JPA가 Entity로 관리한다.
- 기본생성자가 반드시 필요하다. (public 또는 protected)
  - JPA는 내부적으로 리플렉션이 일어나기 때문에 동적으로 객체를 생성해야 한다. 그래서 기본생성자가 필요하다.
- final 클래스, enum, interface, inner 클래스를 사용하면 안된다.
- 저장할 필드에 final 사용하면 안된다.

### @Table
- 엔티티와 매핑할 테이블 지정
- name : 매핑할 테이블 이름
- catalog : 데이터베이스 catalog 매핑
- schema : 데이터베이스 schema 매핑
- uniqueConstraints(DDL) : DDL 생성 시에 유니크 제약 조건 생성


# 필드와 컬럼 매핑

### 매핑 어노테이션 정리
- `@Column` : 컬럼 매핑
- `@Temporal` : 날짜 타입 매핑
  - 날짜 타입(java.util.Date, java.util.Calender)을 매핑할 때 사용
  - 지금은 사실 필요 없다.
    - Java8에서는 LocalDate, LocalDateTime가 지원되어서 필요없어졌다.
    - Hibernate 최신버전은 LocalDate와, LocalDateTime을 쓰면 된다.
- `@Enumerated` : enum 타입 매핑
  - EnumType.ORDINAL : enum 순서를 데이터베이스에 저장 (기본값)
    - enum의 순서가 저장되기 때문에 enum에 필드가 추가되거나 순서가 바뀌면 데이터가 다 꼬인다. 그러므로 사용하지 않는다.
  - EnumType.STRING : enum 이름을 데이터베이스에 저장
- `@Lob` : BLOB, CLOB 매핑(매핑하는 필드타입이 문자면 CLOB으로 매핑, 나머지는 BLOB 매핑)
- `@Transient` : DB의 컬럼과 매핑하지 않을 때 사용 (DB랑 관계없이 메모리에서만 계산하고 싶을 때)

### @Column
- `name` : 필드와 매핑할 테이블의 컬럼 이름 (기본값: 객체의 필드 이름)
- `insertable/updatable` : 등록, 변경 가능 여부 (기본값: TRUE)
- `nullable(DDL)` : null 값의 허용 여부를 설정한다. false로 설정하면 DDL 생성 시에 not null 제약조건이 붙는다.
- `unique(DDL)` : @Table의 uniqueConstraints와 같지만 한 컬럼에 간단히 유니크 제약조건을 걸 때 사용한다.
- `columnDefinition(DDL)` : 데이터베이스 컬럼 정보를 직접 줄 수 있다. (`varchar(100) default 'EMPTY'`)
- `length(DDL)` : 문자 길이 제약조건, String 타입에만 사용한다. (기본값: 255)
- `precision, scale(DDL)` : BigDecimal 타입에서 사용한다(BigInteger도 사용할 수 있다). precision은 소수점을 포함한 전체 자릿수를, scale은 소수의 자릿수다.
참고로 double, float 타입에는 적용되지 않는다. 정밀한 소수를 다루어야 할 때만 사용한다.
