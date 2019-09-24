# JPA 구동 방식

- JPA는 Persistence라는 클래스가 META-INF/persistence.xml 을 읽어서  
EntityManagerFactory 라는 클래스를 생성한다.
- 그리고 EntityManagerFactory에서 EntityManager들을 생성한다.
- EntityManagerFactory는 어플리케이션 로딩 시점에 딱 하나만 만들어야 한다.
- 그리고 트랜잭션 단위 마다 EntityManager를 생성해서 사용한다.
- EntityManager는 쓰레드 간에 공유하면 안되고 사용하고 버려야 한다.

- JPA의 모든 데이터 변경은 트랜잭션 안에서 실행한다.
- RDB는 데이터 변경을 트랜잭션안에서 실행되도록 다 설계되어 있다. 때문에 트랜잭션을 걸지 않아도 DB가 트랜잭션 개념을 가지고 있기 때문에 트랜잭션 안에서 데이터가 변경된다.


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


# 기본 키 매핑
- `@Id` (직접 할당)
- `@GeneratedValue` (자동 생성)
### GenerationType.IDENTITY
~~~java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
~~~

- 기본키 생성을 데이터베이스에 위임
- 주로 MySQL, PostgreSQL, SQL server, DB2에서 사용
- JPA는 보통 트랜잭션 커밋 시점에 INSERT SQL 실행 (즉, DB에 값이 들어가야 ID값을 알 수 있다.)
  - AUTO_INCREMENT는 데이터베이스 INSERT SQL을 실행한 이후에 ID값을 알 수 있다.
  - 그런데 영속성 컨텍스트에서 관리되려면 무조건 PK값이 있어야 하는데, 이 경우 PK값이 DB에 들어가봐야 알 수 있다.
  - 다시 말해, IDENTITY 전략의 경우 1차 캐시의 @Id 값을 DB에 넣기 전까지 모른다.
  - 때문에 JPA 입장에서 key가 없으니까 값을 넣을 방법이 없었다.
  - **그래서 IDENTITY 전략에서만 예외적으로 `em.persist()`가 호출되는 시점에 DB에 INSERT 쿼리를 날린다. (원래는 commit 하는 시점에 INSERT 쿼리가 날린다.)**
 
- IDENTITY 전략은 em.persist() 시점에 즉시 INSERT SQL을 실행하고 DB에서 식별자 조회

### GenerationType.SEQUENCE
~~~java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE)
private Long id;
~~~

- 데이터베이스 시퀀스 오브젝트 사용
- 테이블마다 시퀀스를 따로 관리하고 싶으면 아래와 같이 `@SequenceGenerator` 사용
- **`em.persist()` 호출되기 전에 DB에서 Sequence를 가지고와서 id에 값을 넣어 주고나서, `em.persist()`가 호출되어 영속성 컨텍스트에 저장된다.**
  - 이 상태에서는 아직 DB에 INSERT 쿼리가 날라가지 않고, 영속성 컨텍스트에 쌓여있다가 트랜잭션 commit 하는 시점에 INSERT 쿼리가 날라간다.
  - 때문에 SEQUENCE 전략은 버퍼링이 가능한 것이다.(IDENTITY에서는 INSERT 쿼리를 날려야 했기 때문에 불가능하다)
  
~~~java
@Entity
@SequenceGenegerator(
        name = "MEMBER_SEQ_GENERATOR",
        sequenceName = "MEMBER_SEQ",
        initialValue = 1,
        allocationSize = 1)
public class Member {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
        generator = "MEMBER_SEQ_GENERATOR")
    private Long id;
}
~~~
- `@SequenceGenerator` 속성
  - `name` : 식별자 생성기 이름 (필수)
  - `sequenceName` : DB에 등록되어있는 시퀀스 이름 (기본값: hibernate_sequence)
  - `initialValue` : DDL 생성 시에만 사용됨, 시퀀스 DDL을 생성할 때 처음 1 시작하는 수를 지정한다. (기본값: 1)
  - `allocationSize` : 시퀀스 한 번 호출에 증가하는 수(성능 최적화에 사용됨, **DB 시퀀스 값이 하나씩 증가하도록 설정되어 있으면 이 값을 반드시 1로 설정해야 한다**)
    - 주의!! allocationSize 기본값: 50
    - 이 속성이 성능 최적화에 쓰이는 이유
      - SEQUENCE 전략을 사용할 경우, Sequence를 매번 DB에서 가지고오는 과정에서 네트워크를 타는데,
      - 이 옵션을 사용하면 DB에 50개를 한 번에 올려놓고, 메모리에서 1개씩 쓰는 것이다.
      - 50개를 모두 사용하면 그 때 next call(call next value for MEMBER_SEQ)을 날려서 또 50개를 올려놓는다.
    - 이론적으로는 50개보다 더 큰 수로 설정하면 성능에 좋지만, 만약 그 사이에 어플리케이션을 내리면 할당되지 않은 번호들이 날라가서 공백이 생긴다. 
    공백이 생긴다고 큰 문제가 되지는 않지만, 50~100 정도로 설정하고 사용하는 것이 바람직하다. 
  - `catalog`, `schema` : DB catalog, schema 이름
  
### GenerationType.TABLE
- 키 생성 전용 테이블을 하나 만들어서 DB 시퀀스를 흉내내는 전략
- 장점: 모든 DB에 적용 가능
- 단점: 성능

~~~java
@Entity
@TableGenerator(
        name = "MEMBER_SEQ_GENERATOR",
        table = "MY_SEQUENCES",
        pkColumnValue = "MEMBER_SEQ", allocationSize = 1)
public class Member {
    
    @Id
    @GeneratedValue(strategy = GeneratedValue.TABLE,
                    generator = "MEMBER_SEQ_GENERATOR")
    private Long id;
}
~~~

- `@TableGenerator` 속성
  - `name` : 식별자 생성기 이름 (필수)
  - `table` : 키생성 테이블명 (기본값: hibernate_sequences)
  - `pkColumnName` : 시퀀스 컬럼명 (기본값: sequence_name)
  - `valueColumnName` : 시퀄스 값 컬럼명 (기본값: next_val)
  - `pkColumnValue` : 키로 사용할 값 이름 (기본값: 엔티티 이름)
  - `initialValue` : 초기 값, 마지막으로 생성된 값이 기준 (기본값: 0)
  - `allocationSize` : 시퀀스 한 번 호출에 증가하는 수(성능 최적화에 사용됨, 기본값: 50)
  - `catalog`, `schema` : DB catalog, schema 이름
  - `uniqueConstraints(DDL)` : 유니크 제약 조건을 지정할 수 있다.
  
  
# 권장하는 식별자 전략
- 기본키 제약 조건: null이면 안되고, 유일하고, 변하면 안된다.
- 미래까지 이 조건을 만족하는 자연키는 찾기 어렵다. 대리키(대체키)를 사용하자.
- 예를 들어 주민등록번호도 기본키로 적절하지 않다.
- **권장: Long형 + 대체키 + 키 생성전략 사용**
- 비즈니스를 키로 끌고오는 것은 절대 권장하지 않는다.

# 단방향 연관관계

![](https://github.com/Integerous/images/blob/master/study/jpa/jpaModeling1.png?raw=true)

- 객체를 테이블에 맞추어 데이터 중심으로 모델링하면 협력관계를 만들 수 없다.

![](https://github.com/Integerous/images/blob/master/study/jpa/jpaModeling2.png?raw=true)

~~~java
@Entity
public class Member {
    
    ...
    
//    @Column(name = "TEAM_ID")
//    private Long teamId;

    @ManyToOne
    @JoinColumn(name = "TEAM_ID") // Member 객체의 team과 MEMBER 테이블의 TEAM_ID(FK)를 연관관계로 매핑한다는 뜻
    private Team team;
}
~~~

![](https://github.com/Integerous/images/blob/master/study/jpa/jpaModeling3.png?raw=true)


# 양방향 연관관계

![](https://github.com/Integerous/images/blob/master/study/jpa/bidirectionalMapping1.png?raw=true)

- 위의 그림과 같이 양방향 매핑에서 테이블 연관관계는 단방향 연관관계와 변함이 없다.
  - 그 이유는 **테이블은 TEAM_ID(FK)와 TEAM_ID(PK)를 조인해서 MEMBER로 TEAM을, TEAM으로 MEMBER를 가져올 수 있기 때문이다.** 
  - 즉, 테이블의 연관관계에서는 외래키 하나로 양방향이 다 있는 것이다. 때문에 테이블의 연관관계에는 방향이라는 개념이 없다.
  - 문제는 객체다. Member에서 Team으로 갈 수 있지만, Team에서 Member로 갈 수 없기 때문에 양방향 연관관계를 걸어줘야 한다.
  
### 객체와 테이블이 관계를 맺는 차이
- 객체 연관관계는 2개
  - 회원 -> 팀 연관관계 1개 (단방향)
  - 팀 -> 회원 연관관계 1개 (단방향)
- 테이블 연관관계 1개
  - 회원 <-> 팀의 연관관계 1개 (양방향)

### 객체의 양방향 관계
- 객체의 양방향 관계는 사실 양방향 관계가 아니라 서로 다른 단방향 관계 2개이다.
- 객체를 양방향으로 참조하려면 단방향 연관관계를 2개 만들어야 한다.

### 테이블의 양방향 연관관계
- 테이블은 외래키 하나로 두 테이블의 연관관계를 관리
- MEMBER.TEAM_ID 외래키 하나로 양방향 연관관계를 가진다.(양쪽으로 조인할 수 있다.)

~~~sql
SELECT *
FROM MEMBER M
JOIN TEAM T ON M.TEAM_ID = T.TEAM_ID

SELECT *
FROM TEAM T
JOIN MEMBER M ON T.TEAM_ID = M.TEAM_ID
~~~

# 연관관계의 주인과 mappedBy

### 연관관계의 주인(Owner)
- 양방향 매핑 규칙
  - 객체의 두 관계중 하나를 연관관게의 주인으로 지정
  - 연관관계의 주인만이 외래키를 관리(등록, 수정)
  - 주인이 아닌 쪽은 읽기만 가능
  - 주인은 mappedBy 속성 사용 X
  - 주인이 아니면 mappedBy 속성으로 주인 지정

### 누구를 주인으로?
- **외래키가 있는 곳을 주인으로 정해라**
  - DB 입장에서는 외래키가 있는 곳이 무조건 N이고, 없는 곳이 1이다.
  - 즉, DB의 N쪽이 연관관계의 주인이 되고, `@ManyToOne` 에서 Many 쪽이 연관관계의 주인이 된다.
  - 자동차와 자동차 바퀴가 있으면 비즈니스적으로는 자동차가 중요하지만, 연관관계의 주인은 자동차 바퀴가 된다.
  
![](https://github.com/Integerous/images/blob/master/study/jpa/relationalMapping1.png?raw=true)
- 위에서는 Member.team이 연관관계 주인
- 연관관계 주인이 `team` 이므로, `List<Member> members`에 값을 넣거나 업데이트해도 소용없다.
- 단, 조회는 가능하다.


### 양방향 매핑시 가장 많이 하는 실수

#### 연관관계의 주인에 값을 입력하지 않음
~~~java
Team team = new Team();
team.setName("TeamA");
em.persist(team);

Member member = new Member();
member.setName("member1");

//역방향(주인이 아닌 방향)만 연관관계 설정
team.getMembers().add(member);
em.persist(member);
~~~

이 경우 Member를 조회하면 외래키인 TEAM_ID의 값은 Null이다.  
그러므로 `member.setTeam("TeamA")`으로 연관관계의 주인인 Member의 team에 값을 세팅해야 한다.  

MappedBy로 설정된 곳은 읽기 전용이기 때문에, JPA에서 update하거나 insert할 때(즉, 변경할 때)는 이 부분을 아예 안본다.


### 양방향 연관관계 주의
- 순수 객체 상태를 고려해서 항상 양쪽에 값을 설정한다.
- 연관관계 편의 메소드를 생성한다.
  - `team.getMembers().add(member)`를 지우고,
  - Member 객체에 setTeam 메서드를 아래와 같이 작성한다.
  ~~~java
  public void setTeam(Team team) {
      this.team = team;
      team.getMembers().add(this);
  }
  ~~~
- 양방향 매핑시 무한 루프를 조심한다.
  - toString()
  - lombok
  - JSON 생성 라이브러리
    - 보통 Entity에서 직접 Response 해버릴 때 자주 발생하는데,
    - Entity가 가진 연관관계가 양방향으로 걸려있는 상황에서, 
    (주로 Controller에서 Entity를 바로 반환하는 경우) Entity를 Json으로 바꿀 때 무한참조가 발생한다.
  - 해결 방법
    1. Lombok으로 toString() 쓰지 않기
    2. Controller에서 Entity를 절대 반환하지 말고, DTO로 변환해서 사용하기

### 양방향 매핑 정리
- 단방향 매핑만으로도 이미 연관관계 매핑은 완료
  - 처음에 단방향 매핑으로 설계를 끝내야 한다.
- 양방향 매핑은 반대 방향으로 조회(객체 그래프 탐색) 기능이 추가된 것일 뿐이다.
- 객체 입장에서 양방향으로 설계해서 좋을 것이 별로 없다.
- 실무에서는 역방향으로 탐색할 일이 많지만, 단방향 매핑을 해두고 필요할 때 양방향 매핑을 추가하면 된다.


# 다양한 연관관계 매핑

### 연관관계 매핑 시 고려사항 3가지
- 다중성(다대일, 일대다, 일대일, 다대다)
- 단방향, 양방향
- 연관관계 주인

### 다대일 [N:1]
### 일대다 [1:N]
- 실무에서는 사용하지 않는다.
- 일대다 단방향은 일대다(1:N)에서 1이 연관관계의 주인
- 테이블의 일대다 관계는 항상 다(N) 쪽에 외래키가 있다.
- 객체와 테이블의 차이 때문에 반대편 테이블의 외래키를 관리하는 특이한 구조
- `@JoinColumn`을 꼭 사용해야 한다. 그렇지 않으면 조인 테이블 방식(중간에 테이블 하나 추가)을 사용한다.
- 일대다 단방향 매핑의 단점
  - **엔티티가 관리하는 외래키가 다른 테이블에 있다.**
  - 연관관계 관리를 위해 추가로 UPDATE SQL 실행
- 일대다 단방향 매핑보다는 다대일 양방향 매핑을 사용하는 것이 낫다.
- 일대다 양방향
  - 이 매핑은 공식적으로 존재하지는 않는다.
  - `@JoinColumn(insertable=false, updatable=false)`
  - 읽기 전용 필드를 사용해서 양방향처럼 사용하는 방법
  - 다대일 양방향을 사용하는 것이 낫다.
  
### 일대일 [1:1]
- 일대일 관계는 그 반대도 일대일
- 주 테이블이나 대상 테이블 중에 외래키 선택 가능
- 외래키에 데이터베이스 유니크 제약조건 추가(필수는 아니지만 안하면 관리가 힘들다)
- 일대일 양방향 관계
  - 다대일 양방향 매핑 처럼 외래 키가 있는 곳이 연관관계의 주인이 된다.
  - 주인이 아닌 쪽에 mappedBy 적용

#### 주 테이블에 외래키
- 주 객체가 대상 객체의 참조를 가지는 것 처럼 주 테이블에 외래키를 두고 대상 테이블을 찾는다.
- 객체지향 개발자 선호
- JPA 매핑 편리
- 장점: 주 테이블만 조회해도 대상 테이블에 데이터가 있는지 확인 가능
- 단점: 값이 없으면 외래키에 null 허용

#### 대상 테이블에 외래키
- 대상 테이블에 외래키가 존재
- 전통적인 데이터베이스 개발자 선호
- 장점: 주 테이블과 대상 테이블을 일대일에서 일대다 관계로 변경할 때 테이블 구조 유지
- 단점: 프록시 기능의 한계로 **지연 로딩으로 설정해도 항상 즉시 로딩된다.**

### 다대다 [N:M]
- 실무에서 사용하면 안된다.
  - 연결테이블이 단순히 연결만 하고 끝나지 않다.
  - 주문시간, 수량 같은 데이터가 들어올 수 있다.
- 관계형 데이터베이스는 정규화된 테이블 2개로 다대다 관계를 표현할 수 없다.
- 연결 테이블을 추가해서 일대다, 다대일 관계로 풀어내야한다.
  - **연결테이블을 엔티티로 승격해서 사용한다.**
- 객체는 컬렉션을 사용해서 객체 2개로 다대다 관계가 가능하다.

### @JoinColumn
- 외래키를 매핑할 때 사용
- 속성
  - `name` : 매핑할 외래키 이름 (기본값 = 필드명 + _ + 참조하는 테이블의 기본키 컬럼명)
  - `referencedColumnName` : 외래키가 참조하는 대상 테이블의 컬럼명 (기본값 = 참조하는 테이블의 기본키 컬럼명)
  - `foreignKey(DDL)` : 외래키 제약조건을 직접 지정할 수 있다. 이 속성은 테이블을 생성할 때만 사용한다.
  - 그 외 (@Column의 속성과 같다.)
    - `unique`
    - `nullable`
    - `insertable`
    - `updatable`
    - `columnDefinition`
    - `table`
    
### @ManyToOne
- 속성
  - `optional` : false로 설정하면 연관된 엔티티가 항상 있어야 한다. (기본값 = TRUE)
  - `fetch` : 글로벌 페치 전략을 설정한다. (@ManyToOne = FetchType.EAGER, @OneToMany = FetchType.LAZY)
  - `cascade` : 영속성 전이 기능을 사용한다.
  - `targetEntity` : 연관된 엔티티 타입정보를 설정한다. 이 기능은 거의 사용하지 않는다. 컬렉션을 사용해도 제네릭으로 타입정보를 알 수 있다.
  
### @OneToMany
- 속성
  - 위와 같고 `optional` 대신 `mappedBy`가 있다. (즉, 연관관계의 주인은 다 쪽이어야 한다.)
  
  
# 상속관계 매핑

- 관계형 데이터베이스는 상속관계가 없다.
- 슈퍼타입-서브타입 관계라는 모델링 기법이 객체 상속과 유사
- 결국 상속관계 매핑은 객체의 상속구조와 DB의 슈퍼타입-서브타입 관계를 매핑하는 것이다.
- 슈퍼타입-서브타입 논리모델을 실제 물리모델로 구현하는 방법
  - 각각 테이블로 변환 -> 조인 전략
  - 통합 테이블로 변환 -> 단일 테이블 전략
  - 서브타입 테이블로 변환 -> 구현 클래스마다 테이블 전략
- 실무에서는 조인 전략을 기본으로 깔고 단일테이블 전략과 트레이드오프를 고려해서 사용한다.
  - 단순하면 단일테이블 전략, 조금이라도 복잡하면 조인 전략 사용 권장. 
  
### 조인 전략
- 조인 전략을 정석이라 생각해야 한다.
- 슈퍼타입 객체(예제에서는 `Item`)에 `@Inheritance(strategy = InheritanceType.JOINED)` 어노테이션을 추가하면 된다.
- `@DiscriminatorColumn` 어노테이션을 추가하면 DTYPE 라는 컬럼이 생성되고, ITEM 객체를 상속받은 엔티티 중 join된 엔티티의 이름이 들어가게 된다.
  - 반드시 사용해야하는 것은 아니지만, 되도록 사용하는 것이 좋다.
  - `@DiscriminatorValue("value")"` 어노테이션으로 DTYPE 대신 value명을 정의할 수 있다.
- 장점
  - 테이블 정규화
  - 외래키 참조 무결성 제약조건 활용가능(예제에서는 Item에 제약조건을 걸어서 하위객체들의 제약조건을 맞출 수 있다.)
  - 저장공간 효율화(정규화가 되어있기 때문에)
- 단점
  - 조회시 조인을 많이 사용, 성능 저하
  - 조회 쿼리가 복잡함
  - 데이터 저장시 INSERT SQL 2번 호출
  
### 단일테이블 전략
- 슈퍼타입 객체(예제에서는 `Item`)에 `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` 어노테이션을 추가.
- `@DiscriminatorColumn`이 없어도 DTYPE은 자동으로 생성된다.
  - 조인전략은 테이블이 분리되어 있지만, 단일테이블 전략은 테이블이 하나이므로 구분하기 위해 필수적으로 DTYPE이 필요하다.
- 장점
  - 조인이 필요없어서 조회 성능이 빠르다.
  - 조회 쿼리가 단순하다.
- 단점
  - 자식 엔티티가 매핑한 컬럼은 모두 null을 허용해야 한다. (데이터 무결성 측면에서 애매하다)
  - 단일 테이블에 모든 것을 저장하므로 테이블이 커질 수 있고, 상황에 따라서 조회 성능이 오히려 느려질 수 있다.

### 구현 클래스마다 테이블 전략
- 슈퍼타입 객체(예제에서는 `Item`)에 `@Inheritance(strategy = Inheritance.TABLE_PER_CLASS` 어노테이션을 추가
- **쓰면 안되는 전략**이다. (DB 설계자와 ORM 전문가 둘 다 추천하지 않는다.)
- 장점
  - 서브 타입을 명확하게 구분해서 처리할 때 효과적이다.
  - not null 제약조건을 사용할 수 있다.
- 단점
  - 여러 자식테이블을 함께 조회할 때 성능이 느리다.(UNION SQL)
  - 자식 테이블을 통합해서 쿼리하기 어렵다.
  

# @MappedSuperclass

- 공통 매핑 정보가 필요할 때 사용(id, name 등)
- DB에는 각각 id와 name이 있지만, 객체 입장에서 속성만 상속받아서 쓰고 싶을 때 사용.
- 상속관계 매핑이 아니다.
- 엔티티가 아니다. 그래서 테이블과 매핑이 되지 않는다.
- 부모클래스를 상속받는 자식클래스에 매핑 정보만 제공한다. (@MappedSuperclass가 붙은 클래스의 타입으로는 조회가 안된다.)
  - 참고로 @Entity 클래스는 Entity나 @MappedSuperclass로 지정한 클래스만 상속할 수 있다.
- 직접 생성해서 사용할 일이 없으므로 추상클래스 권장


~~~java
@MappedSuperclass
public abstract class BaseEntity {

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
~~~

이런식으로 생성한 후, 각 엔티티가 이 클래스를 상속받도록 한다.

~~~java
@Entity
public class Member extends BaseEntity {
    ...
}
~~~

~~~java
@Entity
public class Team extends BaseEntity {
    ...
}
~~~


# 프록시

### 프록시 기초
- `em.find()` vs **`em.getReference()`**
  - `em.find()` : DB를 통해서 실제 Entity 객체 조회
  - `em.getReference()` : DB 조회를 미루는 가짜(프록시) Entity 객체 조회 (DB에 쿼리가 안나가는데 객체가 조회되는 것)
    - 하지만 `em.getReference()`로 찾은 것을 실제 사용할 때는 DB에 쿼리가 나간다.

### 프록시 특징
- 실제 클래스를 상속받아서 만들어짐 (하이버네이트가 내부 라이브러리를 사용해서)
- 실제 클래스와 겉 모양이 같다.
- 사용하는 입장에서는 진짜 객체인지 프록시 객체인지 구분하지 않고 사용하면 된다.(이론상)
- 프록시 객체는 실제 객체의 참조(target)를 보관
- 프록시 객체를 호출하면 프록시 객체는 실제 객체의 메서드 호출

### 프록시 객체의 초기화
![](https://github.com/Integerous/images/blob/master/study/jpa/jpa_proxy1.png?raw=true)

- 영속성 컨텍스트를 통해서 초기화 요청을 하는 것이 포인트
- 프록시 객체는 처음 사용할 때 한 번만 초기화
- 프록시 객체를 초기화 할 때, 프록시 객체가 실제 Entity로 바뀌는 것이 아니다. **초기화되면 프록시 객체를 통해서 실제 Entity에 접근이 가능해지는 것**이다.
- 프록시 객체는 원본 Entity를 상속받는다. 때문에 타입 체크시 주의해야 한다.(`==` 비교 실패, 대신 `instance of` 사용)
- 영속성 컨텍스트에 찾는 Entity가 이미 있으면, `em.getReference()`를 호출해도 실제 Entity를 반환한다.
  - 첫번째 이유 = 성능
  - 두번째 이유 = JPA는 같은 트랜잭션 안에서 `==` 비교시 True를 보장해줘야 하기 때문에
    - 두번째 이유 때문에 `em.getReference()` 이후에 `em.find()`를 하면 둘 다 프록시 객체를 반환한다. (== 비교시 True 보장을 위해)
    - 즉, 실제 개발할 때 프록시 객체인지 실제 객체인지 신경쓰지 않고 개발할 수 있게 되는 것이다.
- 영속성 컨텍스트의 도움을 받을 수 없는 준영속 상태일 때, 프록시를 초기화하면 문제 발생. (하이버네이트는 org.hibernate.LazyInitializtionException 예외 발생)

### 프록시 확인
- 프록시 인스턴스의 초기화 여부 확인
  - `PersistenceUnitUtil.isLoaded(Object entity)`
  - `emf.getPersistenceUnitUtil().isLoaded(Object entity)`
- 프록시 클래스 확인 방법
  - `entity.getClass().getName()` 출력
- 프록시 강제 초기화
  - `org.hibernate.Hibernate.initialize(entity);`
  - 참고로 JPA 표준은 강제 초기화 없음 (강제 호출: member.getName())


# 즉시로딩과 지연로딩
- 즉시로딩 사용시 실제 객체를 조인해서 한방 쿼리로 조회한다. (초기화가 필요없다.)
  - 예를 들어 Member를 가지고 올 때 Team도 가지고 온다.
- 지연로딩 사용시 프록시로 조회한다.
- 실무에서는 가급전 지연로딩만 사용 권장
  - 즉시로딩을 적용하면 예상하지 못한 SQL 발생
  - 즉시로딩은 JPQL에서 N+1 문제를 일으킨다.
  - `@ManyToOne`, `@OneToOne` 은 디폴트가 EAGER 이므로 LAZY로 바꾼다.
  - `@OneToMany`, `@ManyToMany`는 디폴트가 LAZY

### N+1 문제
1. JPQL을 사용할 때 `em.createQuery("select m from Member m", Member.class)`에서 우선 select 쿼리를 그대로 해석해서 일단 Member를 조회하는 select 쿼리가 나간다.
2. 이후에 Member와 @ManyToOne 관계인 Team의 로딩전략이 EAGER인 것을 확인하고, Team을 조회하는 쿼리가 또 나간다.
3. 그런데 이때 문제는 Member들이 속한 Team의 개수(N개)만큼 Team을 조회하는 쿼리가 나가는 것이다.
4. 그래서 최초의 Member를 조회하는 쿼리 1개와 Team을 조회하는 N개 만큼의 쿼리가 나간다고 하여 N+1 문제라고 한다. 

### N+1 문제 해결법
우선 모든 연관관계를 지연로딩으로 깐다.

1. Fetch Join
~~~java
em.createQuery("select m from Member m join fetch m.team", Member.class);
~~~
  - LAZY로 설정되어있어도, 위의 쿼리에 따라 Member와 Team을 한방 쿼리로 가지고 온다.
2. Entity Graph (어노테이션)
3. Batch Size (N+1 -> 1+1)


# 영속성 전이(CASCADE)
- 특정 엔티티를 영속상태로 만들 때 연관된 Entity도 함께 영속 상태로 만들 때 사용
- 영속성 전이는 연관관계를 매핑하는 것과 아무 관련 없다.
- Parent 뿐만 아니라 다른 객체가 Child와 연관이 있다면 사용하면 안된다. (단일 Entity에 종속적일 때 사용해야 한다.)
- 

~~~java
@Entity
public class Parent {
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL) // Parent를 persist할 때 childList의 child들도 persist 된다.
    private List<Child> childList = new ArrayList<>();    
}
~~~

### Cascade 종류
- **ALL : 모두 적용** (모든 라이프사이클 맞출 때 사용)
- **PERSIST : 영속** (저장할 때만 라이프사이클 맞출 때 사용)
- **REMOVE : 삭제**
- MERGE : 병합
- REFRESH
- DETACH


# 고아 객체
- 고아 객체 제거: 부모 Entity와 연관관계가 끊어진 자식 Entity를 자동으로 삭제
- `orphanRemoval = true`

~~~java
Parent parent1 = em.find(Parent.class, id);
parent1.getChildren().remove(0); //자식 엔티티를 컬렉션에서 제거
~~~

- 위의 경우 `DELETE FROM CHILD WHERE ID = ?` 쿼리가 나간다.
- 결국 `CascadeType.ALL` 혹은 `CascadeType.REMOVE` 처럼 동작한다.

### 고아객체 제거 시 주의점
- **참조하는 곳이 하나일 때 사용해야한다.**
- 특정 Entity가 개인 소유할 때 사용한다.
- `@OneToOne`, `@OneToMany`만 가능

### `CascadeType.ALL` + `orphanRemoval = true`
- 스스로 생명주기를 관리하는 Entity는 em.persist()로 영속화하고 em.remove()로 제거할 수 있다.
- 그런데 두 옵션을 모두 활성화 하면 부모 Entity를 통해서 자식의 생명주기를 관리할 수 있다.
- 도메인 주도 설계(DDD)의 Aggregate Root 개념을 구현할 때 유용하다.

# 값 타입

### JPA의 데이터 타입 분류
- 최상위에는 엔티티 타입과 값 타입이 있다.

#### 엔티티 타입
- @Entity로 정의하는 객체
- 데이터가 변해도 식별자로 지속해서 추적 가능 (회원 Entity의 키나 나이 값을 변경해도 식별자로 인식 가능)

#### 값 타입
- int, integer, String 처럼 단순히 값으로 사용하는 자바 기본 타입이나 객체
- 식별자가 없고 값만 있으므로 변경시 추적 불가 (숫자 100을 200으로 변경하면 완전히 다른 값으로 대체)

### 값 타입 분류

#### 기본값 타입
- 자바 기본 타입(int, double)
- 래퍼 클래스(Integer, Long)
- String
#### 임베디드 타입(embedded type, 복합 값 타입)
#### 컬렉션 값 타입(collection value type)


### 기본값 타입
- ex) String name, int age
- 생명주기를 엔티티에 의존
  - 회원을 삭제하면 이름, 나이 필드도 함께 삭제
- 값 타입은 공유하면 안된다.
  - 회원 이름 변경시 다른 회원의 이름도 함께 변경되면 안된다.
- 자바의 primitive 타입은 절대 공유되지 않는다.
  - primitive 타입은 항상 값을 복사한다.
  ~~~java
  int a = 10;
  int b = a;

  a = 20;
  
  // 결과 
  // a = 20
  // b = 10
  // 즉, int b = a; 에서 a의 값이 복사가 되서 b에 담기는 것이다. 
  ~~~
  - Integer 같은 래퍼 클래스나 String 같은 특수한 클래스는 공유 가능한 객체이지만 변경되지 않는다.
  ~~~java
    Integer a = new Integer(10);
    Integer b = a;
    
    a.setValue(20); //setValue()가 있다고 치고.
  
    // 결과 
    // a = 20
    // b = 20
    // 같은 인스턴스를 공유하기 때문이다. 하지만 값을 변경할 여지가 없어서 사이드이펙트가 없다.
  ~~~
  
### 임베디드 타입(복합값 타입)
- 새로운 값 타입을 직접 정의할 수 있다.
- JPA는 임베디드 타입(embedded type)이라고 한다.
- 주로 기본값 타입을 모아서 만들어서 복합값 타입이라고도 한다.
- **int, String 처럼 임베디드 타입도 값 타입**이다. (Entity가 아니다.)

#### 임베디드 타입 예시
![](https://github.com/Integerous/images/blob/master/study/jpa/jpa_embeddedType1.png?raw=true)

#### 임베디드 타입 사용법
- `@Embeddedable` : 값 타입을 정의하는 곳에 표시
- `@Embedded` : 값 타입을 사용하는 곳에 표시
- 기본 생성자 필수

#### 임베디드 타입 장점
- 재사용 가능
- 높은 응집도
- Period.isWork() 처럼 해당 값 타입만 사용하는 의미있는 메서드를 만들 수 있다.
- 임베디드 타입을 포함한 모든 값 타입은, 값 타입을 소유한 엔티티에 생명주기를 의존한다.

#### 임베디드 타입과 테이블 매핑
- 임베디드 타입은 Entity의 값일 뿐이다.
- **임베디드 타입을 사용하기 전과 후에 매핑하는 테이블은 같다.**
- 객체와 테이블을 아주 세밀하게(fine-grained) 매핑하는 것이 가능하다.
- 잘 설계한 ORM 어플리케이션은 매핑한 테이블의 수 보다 클래스의 수가 더 많다. 


### @AttributeOverride
- 한 엔티티에서 같은 값 타입을 사용하면?
  ~~~java
  @Embedded
  private Address homeAddress;

  @Embedded // 한 엔티티에서 같은 값 타입을 사용하면 중복된 컬럼 매핑으로 에러가 나므로 아래와 같이 설정한다.
  @AttributeOverrides({
          @AttributeOverride(name = "city",
                  column = @Column(name = "WORK_CITY")),
          @AttributeOverride(name = "street",
                  column = @Column(name = "WORK_STREET")),
          @AttributeOverride(name = "zipcode",
                  column = @Column(name = "WORK_ZIPCODE"))
  })
  private Address workAddress;
  ~~~
  
### 임베디드 타입과 null
- 임베디드 타입의 값이 null이면 매핑한 컬럼 값은 모두 null


# 값 타입과 불변 객체
- 값 타입은 복잡한 객체 세상을 조금이라도 단순화하려고 만든 개념이다.
- 따라서 값 타입은 단순하고 안전하게 다룰 수 있어야 한다.

### 값 타입 공유 참조
- 임베디드 타입 같은 값 타입을 여러 엔티티에서 공유하면 부작용이 발생한다.
    ~~~java
    ...
    Address address = new Address("city", "street", "10000");
    
    Member member1 = new Member();
    member1.setUsername("member1");
    member1.setHomeAddress(address);
    em.persist(member1);
    
    Member member2 = new Member();
    member2.setUsername("member2");
    member2.setHomeAddress(address);
    em.persist(member2);
    
    member1.getHomeAddress().setCity("newCity"); // 이 경우 member1과 member2의 city가 모두 변경된다.
    ~~~
- 위와 같이 값 타입의 실제 인스턴스인 값을 공유하는 것은 위험하므로, 아래와 같이 값(인스턴스)을 복사해서 사용해야 한다.
    ~~~java
    ...
    Address address = new Address("city", "street", "10000");
    
    Member member1 = new Member();
    member1.setUsername("member1");
    member1.setHomeAddress(address);
    em.persist(member1);
  
    //이 부분 추가
    Address copyAddress = new Address(address.getCity(), address.getStreet(), address.getZip);
    
    Member member2 = new Member();
    member2.setUsername("member2");
    member2.setHomeAddress(copyAddress); // copyAddress 사용
    em.persist(member2);
    
    member1.getHomeAddress().setCity("newCity"); // 이 경우 member1과 member2의 city가 모두 변경된다.
    ~~~
    
- 위와 항상 값을 복사해서 사용하면 공유 참조로 인해 발생하는 부작용을 피할 수 있지만,
- 임베디드 타입처럼 직접 정의한 값 타입은 primitive 타입이 아니라 객체 타입이라 참조 값을 직접 대입하는 것을 막을 방법이 없으므로 공유 참조를 피할 수 없다.


### 불변 객체
- 객체 타입을 수정할 수 없게 만들면 공유참조의 부작용을 원천 차단할 수 있다.
- 그러므로 값 타입은 불변 객체(immutable object)로 설계해야 한다.
- 불변객체는 **생성 이후에 절대 값을 변경할 수 없는 객체**
- 생성자로만 값을 설정하고 수정자(Setter)를 만들지 않으면 된다.
- Integer와 String은 Java가 제공하는 대표적인 불변객체이다.


### 값 타입 비교
- 값 타입은 인스턴스가 달라도 그 안에 값이 같으면 같은 것으로 봐야 한다.
- 동일성(Identity) 비교
  - 인스턴스의 참조 값을 비교
  - `==` 사용
- 동등성(Equivalence) 비교
  - 인스턴스의 값을 비교
  - `equals()` 사용
- **값 타입은 a.equals(b)를 사용해서 동등성 비교를 해야된다.** (equals(), hashcode() 메서드를 오버라이드 해야 함)
- 값 타입의 equals() 메소드를 적절하게 재정의(주로 모든 필드 사용)

# 값 타입 컬렉션
- 값 타입을 컬렉션에 담아서 쓰는 것 (값 타입을 하나 이상 저장할 때 사용)
- `@ElementCollection`, `@CollectionTable` 사용
- 관계형 데이터베이스는 기본적으로 테이블 안에 컬렉션을 담을 수 있는 구조가 없고, 값만 넣을 수 있다.
  - 때문에 아래와 같이 별도의 테이블을 생성해야 한다.
  - 이 때, 값 타입의 값들을 모두 PK로 만들어야 한다. 그렇지 않고 ID를 만들어서 PK로 사용하면 Entity가 된다.
![](https://github.com/Integerous/images/blob/master/study/jpa/jpa_valuetype_collection.png?raw=true)

~~~java
@Entity
public class Member {
    ...
    ...
    
    @Embedded
    private Address homeAddress;
    
    @ElementCollection // 값타입 컬렉션 매핑 (기본값 = LAZY)
    @CollectionTable(name = "FAVORITE_FOOD", // 테이블명 지정
        joinColums = @JoinColumn(name = "MEMBER_ID")) // MEMBER_ID를 외래키로 잡음
    @Column(name = "FOOD_NAME") // 컬럼명 지정
    private Set<String> favoriteFoods = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "ADDRESS",
        joinColumns = @JoinColumn(name = "MEMBER_ID"))
    private List<Address> addressHistory = new ArrayList<>();
}
~~~

- **값 타입 컬렉션은 그 자체로 라이프사이클이 없다.**
  - 때문에 아래와 같이 Member만 persist()해도 값 타입 컬렉션은 다른 테이블임에도 라이프사이클이 Member와 같이 돌아간다.
  - 즉, Member에 의존한다.
  - 값 타입 컬렉션은 `@OneToMany(cascade = ALL, orphanRemoval = true)`와 같이 영속성 전이와 고아 객체 제거 기능을 필수로 가진다고 볼 수 있다.
- `@ElementCollection`은 LazyLoading이 디폴트다.
  - 즉, 값 타입 컬렉션은 기본적으로 지연로딩 전략이 사용된다.
  - 위의 코드에서 Member만 조회할 경우 임베디드 타입인 homeAddress는 같이 조회되지만, 값 타입 컬렉션인 favoriteFoods와 addressHistory는 조회되지 않는다.
~~~java
...

Member member = new Member();
member.setUsername("member1");
member.setHomeAddress(newe Address("homeCity", "street");

member.getFavoriteFoods().add("치킨");
member.getFavoriteFoods().add("피자");
member.getFavoriteFoods().add("족발");

member.getAddressHistory().add(new Address("old1", "street"));
member.getAddressHistory().add(new Address("old2", "street"));

em.persist(member);
~~~

### 값 타입 수정
- 값 타입의 필드 하나를 수정하면 안되고, 값 타입 자체를 교체해야 한다.

~~~java
//homeCity -> newCity
findMember.getHomeAddress().setCity("newCity"); // 이렇게 하면 안된다. (사이드이펙트 발생함)

findMember.setHomeAddress(new Address("newCity", ...)); // 값 타입의 수정은 이렇게 해야 한다.
~~~

### 값 타입 컬렉션 수정
~~~java
//값 타입 컬렉션 :
Set<String> favoriteFoods

//컬렉션 안의 치킨->한식
findMember.getFavoriteFoods().remove("치킨");
findMember.getFavoriteFoods().add("한식");

//위의 값 타입 컬렉션의 String도 값 타입이기 때문에 통째로 갈아끼워야 한다. 
~~~

~~~java
// 아래 주소에서 old1 -> new1 으로 바꾸려면
Address address = new Address("old1, street, 1000");

// 우선 아래와 같은 방식으로 지워야 한다.
// 그런데 이 때, Address에 equals()와 hashCode() 메서드가 구현이 안되어 있으면 망한다.
findMember.getAddressHistory().remove(new Address("old1, street, 1000"));

// 그리고 새로 추가한다.
findMember.getAddressHistory().add(new Address("new1, street, 1000"));
~~~


### 값 타입 컬렉션의 제약사항
- **값 타입은 Entity와 다르게 식별자 개념이 없다.**
- 때문에 값을 변경하면 추적이 안된다.
- 그래서 값 타입 컬렉션에 변경 사항이 발생하면, 주인 Entity와 연관된 모든 데이터를 삭제하고, 값 타입 컬렉션에 있는 현재 값을 모두 다시 저장한다.
- 값 타입 컬렉션을 매핑하는 테이블은 모든 컬럼을 묶어서 기본키를 구성해야 한다. (null 입력 X, 중복 저장 X)

### 값 타입 컬렉션 대안
- 실무에서는 상황에 따라 값 타입 컬렉션 대신에 일대다 관계를 고려
- 일대다 관계를 위한 Entity를 만들고, 여기에 값 타입을 사용
- 영속성 전이(Cascade) + 고아 객체 제거를 사용해서 값 타입 컬렉션 처럼 사용

~~~java
@Entity
@Table(name="ADDRESS")
@AllArgsConstructor
@Getter
@Setter
public class AddressEntity {
    
    @Id @GeneratedValue
    private Long id;
    
    private Address address; // 얘는 값 타입
}
~~~

~~~java
@Entity
public class Member {
    ...
    ...
    
    @Embedded
    private Address homeAddress;
    
    @ElementCollection // 값타입 컬렉션 매핑 (기본값 = LAZY)
    @CollectionTable(name = "FAVORITE_FOOD", // 테이블명 지정
        joinColums = @JoinColumn(name = "MEMBER_ID")) // MEMBER_ID를 외래키로 잡음
    @Column(name = "FOOD_NAME") // 컬럼명 지정
    private Set<String> favoriteFoods = new HashSet<>();
    
    // @ElementCollection
    // @CollectionTable(name = "ADDRESS",
    //    joinColumns = @JoinColumn(name = "MEMBER_ID"))
    //private List<Address> addressHistory = new ArrayList<>();
    
    /**
    * 위와 같이 값타입 컬렉션으로 매핑하는 대신
    * 아래와 같이 Entity로 매핑한다.
    * AddressEntity 쪽에 @ManyToOne을 써서 (oneToMany - manyToOne) 기본매핑을 사용해도 되지만,
    * 이 케이스에서는 cascade.ALL과 orphanRemoval=true 로 잡고, 
    * @JoinColumn을 써서 일대다 단방향 매핑을 한다.
    * 이렇게 사용하면, 위에서 값타입으로 매핑하는 것보다 훨씬 활용도가 높아진다.
    */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "MEMBER_ID")
    private List<AddressEntity> addressHistory = new ArrayList<>();
}
~~~

### 값 타입 컬렉션은 언제쓰는가?
- 단순할 때 사용
  - 예를 들어 select box에 [치킨, 피자]를 멀티로 선택할 수 있도록 할 때 사용
  - 값이 바뀌어도 업데이트 할 필요가 없을 때 값 타입 컬렉션을 사용하고,
  - 왠만하면 사용하지 않는다.
  
### 정리
- **Entity 타입 특징**
  - 식별자 O
  - 생명 주기 관리
  - 공유
- **값 타입 특징**
  - 식별자 X
  - 생명 주기를 엔티티에 의존
  - 공유하지 않는 것이 안전(복사해서 사용)
  - 불변 객체로 만드는 것이 안전
- 즉, 식별자가 필요하고, 지속해서 값을 추적, 변경해야 한다면 그것은 값 타입이 아닌 Entity이다.


# JPQL

- JPQL은 SQL을 추상화한 객체지향 쿼리이다. (한마디로 JPQL = 객체지향 SQL)
  - JPQL은 Entity 객체를 대상으로 쿼리하고,
  - SQL은 DB 테이블을 대상으로 쿼리한다.
- **검색을 할 때에도 테이블이 아닌 Entity 객체를 대상으로 검색**한다.
- 모든 DB 데이터를 객체로 변환해서 검색하는 것은 불가능하다.
- 어플리케이션이 필요한 데이터만 DB에서 불러오려면 결국 검색 조건이 포함된 SQL이 필요하다.
  - DB의 테이블을 대상으로 쿼리를 날리면 해당 DB에 종속적인 설계가 된다.
  - 때문에 Entity 객체를 대상으로 쿼리를 할 수 있는 JPQL이 제공된 것이다.
- 방언을 바꿔도 JPQL을 바꿀 필요가 없다. (특정 데이터베이스 SQL에 의존하지 않는다.)

~~~java
List<Member> result = em.createQuery(
        "select m From Member m where m.username like '%kim%'", Member.class 
).getResultList();
// 여기서 Member는 테이블이 아니라 엔티티
~~~

### Criteria
- 문자가 아닌 Java코드로 JPQL을 작성하므로 컴파일 오류를 잡아주고, 동적쿼리를 작성하기 수월하다.
- 하지만 SQL스럽지 않아 이해하기 힘들어서 실무에서 안쓴다.
- 대신 QueryDSL 사용 권장

~~~java
// Criteria 사용 준비
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Member> query = cb.createQuery(Member.class);

// 루트 클래스 (조회를 시작할 클래스)
Root<Member> m = query.from(Member.class);

// 쿼리 생성
CriteriaQuery<Member> cq = query.select(m).where(cb.equal(m.get("username"), "kim"));
List<Member> resultList = em.createQuery(cq).getResultList();
~~~

### QueryDSL
- 문자가 아닌 Java코드로 JPQL을 작성할 수 있다.
- JPQL 빌더 역할
- 컴파일 시점에 문법 오류를 찾을 수 있다.
- 동적쿼리 작성에 편리하다.
- 실무 사용 권장

~~~java
// JPQL
// select m from Member m where m.age > 18
JPAFactoryQuery query = new JPAQueryFactory(em);
QMember m = QMember.member;

List<Member> list =
        query.selectFrom(m)
             .where(m.age.gt(18))
             .orderBy(m.name.desc())
             .fetch();
~~~

### 네이티브 SQL
- JPA가 제공하는 SQL을 직접 사용하는 기능
- JPQL로 해결할 수 없는 특정 DB에 의존적인 기능
  - 예) 오라클 CONNECT BY, 특정 DB만 사용하는 SQL 힌트
  
~~~java
String sql = "SELECT ID, AGE, TEAM_ID, NAME FROM MEMBER WHERE NAME = 'kim'";

List<Member> resultList = 
            em.createNativeQuery(sql, Member.class).getResultList();
~~~

### JDBC, SpringJdbcTemplate 등
- JPA를 사용하면서 JDBC 커넥션을 직접 사용하거나, SpringJdbcTemplate, MyBatis 등을 함께 사용 가능
- 단, **영속성 컨텍스트를 적절한 시점에 강제로 flush 필요**
  - flush가 호출되는 상황 2가지
    - commit 될 때
    - query 날라갈 때(.createQuery 또는 .createNativeQuery 등)
  - JPA를 우회해서 SQL을 실행하기 직전에 영속성 컨텍스트 수동 플러시
  

# JPQL 문법
- `select m from Member as m where m.age > 18`
- Entity와 속성은 대소문자를 구분한다. (Member, age)
- JPQL 키워드는 대소문자를 구분하지 않는다. (SELECT, FROM, where)
- Entity의 이름 사용. 테이블 이름이 아니다.
- **별칭(m)은 필수** (as는 생략 가능)

### TypeQuery, Query
- TypeQuery는 반환타입이 명확할 때 사용
  ~~~java
  TypedQuery<Member> query =
      em.createQuery("SELECT m FROM Member m", Member.class)
  ~~~
- Query는 반환타입이 명확하지 않을 때 사용
  ~~~java
  Query query =
      em.createQuery("SELECT m.username, m.age from Member m");
  ~~~
  
### 결과 조회
- `query.getResultList()`
  - 결과가 1개 이상일 때 사용
  - 리스트 반환
  - 결과가 없으면 빈 리스트 반환
- `query.getSingleResult()`
  - 결과가 1개일 때 사용
  - 단일 객체 반환
  - 결과가 없으면 `javax.persistence.NoResultException`
    - Spring Data JPA는 null 반환
  - 둘 이상이면 `javax.persistence.NonUniqueResultException` 발생
  
### 파라미터 바인딩
- 이름 기준
  ~~~java
  select m from Member m where m.username=:username
  query.setParameter("username", usernameParam);
  ~~~
- 위치 기준
  ~~~java
  select m from Member m where m.username=?1
  query.setParameter(1, usernameParam);
  ~~~
  
### 프로젝션
- SELECT 절에 조회할 대상을 지정하는 것
- 프로젝션 대상
  - Entity
  - 임베디드 타입
  - 스칼라 타입(숫자, 문자등 기본 데이터 타입)
  
- `SELECT m FROM Member m` : Entity 프로젝션
- `SELECT m.team FROM Member m` : Entity 프로젝션
  - 위와 같이 작성하면 join 쿼리가 나간다는 사실을 알기 힘들다.
  - 그러므로 명시적 조인 `SELECT t FROM Member m join m.team t` 을 사용하는 것이 좋다.
- `SELECT m.address FROM Member m` : 임베디드 타입 프로젝션
- `SELECT m.username, m.age FROM Member m` : 스칼라 타입 프로젝션


~~~java
Member member = new Member();
member.setAge(10);
em.persist(member);
em.flush();
em.clear();

/**
* 영속성 컨텍스트가 비어있는 이 상태에서 아래와 같이 Entity 프로젝션을 하면,
* 대상이 된 Entity가 영속성 컨텍스트에서 모두 관리된다.
*/
List<Member> result = em.createQuery("select m from Member m", Member.class)
                        .getResultList();

Member findMember = result.get(0);
findMember.setAge(20); // 조회 결과: 20.
~~~

### 프로젝션 여러 값 조회
1. Query 타입으로 조회
  ~~~java
  List resultList = em.createQuery("select m.username, m.age from Member m")
                      .getResultList();
  
  Object o = resultList.get(0);
  Object[] result = (Object[]) o;
  ~~~
2. Object[] 타입으로 조회
  ~~~java
  List<Object[]> resultList = em.createQuery("select m.username, m.age from Member m", Object[].class)
                                .getResultList();

  Object[] result = resultList.get(0);
  ~~~
3. new 명령어로 조회
  - 이 방법이 가장 깔끔하다.
  - 단순 값을 DTO로 바로 조회한다.
  - 패키지 명을 포함한 전체 클래스명을 입력해야 한다.
  - 순서와 타입이 일치하는 생성자가 필요하다.
  - 우선 MemberDTO 정의
    ~~~java
    @Getter
    @Setter
    public class MemberDTO {
        
        private String username;
        private int age;
    
        public MemberDTO(String username, int age) {
            this.username = username;
            this.age = age;
        }
    }
    ~~~
  ~~~java
  List<MemberDTO> resultList = em.createQuery("select new jpql.MemberDTO(m.username, m.age) from Member m", MemberDTO.class)
                                .getResultList();

  MemberDTO memberDTO = resultList.get(0);
  ~~~
  - 여기서 jpql.MemberDTO의 jpql은 패키지명이다.
  - 때문에 패키지명이 길어지면 다 적어줘야 하는 것이 단점이다. (하지만 이 문제는 QueryDSL에서 극복할 수 있다.) 


### 페이징 API
- 페이징은 사실 **몇 번째부터 몇 개 가져올래?**가 전부다.
- 그래서 JPA는 페이징을 다음 두 API로 추상화했다.
  - `setFirstResult(int startPosition)` : 조회 시작 위치(0부터 시작)
  - `setMaxResult(int maxResult)` : 조회할 데이터 수
  
~~~java
String jpql = "select m from Member m order by m.name desc";
List<Member> resultList = em.createQuery(jpql, Member.class)
        .setFirstResult(10)
        .setMaxResults(20)
        .getResultList();
~~~

### 조인
- SQL 조인과 실행되는 것은 똑같지만, JPQL의 조인은 Entity를 중심으로 조인쿼리가 나간다.
- **내부 조인**
  - `SELECT m FROM Member m [INNER] JOIN m.team t`
- **외부 조인**
  - `SELECT m FROM Member m LEFT [OUTER] JOIN m.team t`
- **세타 조인**
  - 연관관계가 없는 것들을 비교할 때 사용
  - `SELECT count(m) FROM Member m, Team t WHERE m.username=t.name`
- **ON절을 활용한 조인**
  - 조인 대상 필터링
    - (예시) 회원과 팀을 조인하면서, 팀 이름이 A인 팀만 조인
      - JPQL
        - `SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'A'`
      - 실제 나가는 SQL
        - `SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='A'`
  - **연관관계 없는 Entity를 외부 조인** 할 수 있다.(Hibernate 5.1^)
    - (예시) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
      - JPQL
        - `SELECT m, t FROM Member m LEFT JOIN Team t ON m.username = t.name`
      - 실제 나가는 SQL
        - `SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name`


### 서브 쿼리

~~~sql
// 나이가 평균보다 많은 회원
SELECT m FROM Member m
WHERE m.age > (SELECT avg(m2.age) FROM Member m2) 
// m 대신 m2 사용한 점 주목.
// 메인쿼리와 서브쿼리가 관계없어야 서브쿼리 성능에 좋다.
~~~

~~~sql
// 한 건이라도 주문한 고객
SELECT m FROM Member m
WHERE (SELECT count(o) FROM Order o WHERE m = o.member) > 0
// m을 그대로 사용하면 성능이 잘 안나옴.
~~~

### 서브 쿼리 지원 함수
- `[NOT] EXISTS (subquery)` : 서브쿼리에 결과가 존재하면 참
  - {ALL | ANY | SOME} (subquery)
  - ALL = 모두 만족하면 참
  - ANY, SOME = 같은 의미, 조건을 하나라도 만족하면 참
- `[NOT] IN (subquery)` : 서브쿼리의 결과 중 하나라도 같은 것이 있으면 참 

#### 예시 
- 팀A 소속인 회원
    ~~~sql
    SELECT m FROM Member m
    WHERE exists(SELECT t FROM m.team t WHERE t.name = '팀A')
    ~~~
- 전체 상품 각각의 재고보다 주문량이 많은 주문들
    ~~~sql
    SELECT o FROM Order o
    WHERE o.orderAmount > ALL (SELECT p.stockAmount FROM Product p)
    ~~~
- 어떤 팀이든 팀에 소속된 회원
    ~~~sql
    SELECT m FROM Member m
    WHERE m.team = ANY (SELECT t FROM Team t)
    ~~~
    
### JPA 서브쿼리 한계
- JPA 표준스펙에는 WHERE, HAVING 절에서만 서브쿼리 사용 가능
  - 보통 구현체를 하이버네이트를 쓰는데, 하이버네이트에서는 SELECT 절에서도 가능
    - `SELECT (SELECT avg(m1.age) FROM Member m1) as avgAge FROM Member`
- **FROM 절의 서브쿼리는 현재 JPQL에서 불가능**
  - FROM 절의 서브쿼리 = `SELECT mm FROM (SELECT m.age FROM Member m) as mm`
  - 조인으로 풀 수 있으면 풀어서 해결
  - 안되면 네이티브 쿼리를 쓰거나, 어플리케이션에서 조립하거나, 쿼리를 2번 날린다.
  
### JPQL 타입 표현
- 문자: 'HELLO', 'She''s'
- 숫자: 10L(Long), 10D(Double), 10F(Float)
- Boolean: TRUE, FALSE
- ENUM : jpabook.MemberType.Admin (패키지명 포함)
- Entity 타입 : TYPE(m) = Member (상속 관계에서 사용)

~~~sql
SELECT m.username, 'HELLO', TRUE FROM Member m
WHERE m.type = jpql.MemberType.USER
~~~

~~~sql
SELECT i FROM Item i WHERE type(i) = Book
~~~

### 조건식
- 기본 CASE 식
  ~~~sql
  SELECT
      CASE WHEN m.age <= 10 THEN '학생요금'
           WHEN m.age >= 60 THEN '경로요금'
           ELSE '일반요금'
      END
  FROM Member m
  ~~~
- 단순 CASE 식
  ~~~sql
  SELECT
      CASE t.name
          WHEN '팀A' THEN '인센티브110%'
          WHEN '팀B' THEN '인센티브120%'
          ELSE '인센티브105%'
      END
  FROM Team t
  ~~~
- COALESCE
  - 하나씩 조회해서 null이 아니면 반환
  - 예시) 사용자 이름이 없으면 '이름 없는 회원'을 반환
  ~~~sql
  SELECT COALESCE(m.username, '이름 없는 회원') FROM Member m
  ~~~
- NULLIF
  - 두 값이 같으면 null 반환, 다르면 첫번째 값 반환
  - 예시) 사용자 이름이 '관리자'면 null을 반환하고 나머지는 본인의 이름을 반환
  ~~~sql
  SELECT NULLIF(m.username, '관리자') FROM Member m
  ~~~
  
### JPQL 기본 함수
- JPQL 표준함수(DB에 관계없이 쓰면 된다.)
  - CONCAT
  - SUBSTRING
  - TRIM
  - LOWER, UPPER
  - LENGTH
  - LOCATE
    - `SELECT LOCATE('de', 'abcdef') FROM Member m`
    - `abcdef`에서 `de`가 시작되는 위치 4 반환
  - ABS, SQRT, MOD
  - SIZE, INDEX(JPA 용도)
- 사용자 정의 함수
  - 하이버네이트는 사용 전 방언에 추가해야 한다.
    - 사용하는 DB 방언을 상속받고, 사용자 정의 함수를 등록한다.
  - `SELECT FUNCTION('group_concat', m.username) FROM Member m` 또는
  - `SELECT group_concat(m.username) FROM Member m`
    - 위 함수를 사용하기 위해서 아래와 같이 함수 정의
      ~~~java
      public class MyH2Dialect extends H2Dialect {
          
          public MyH2Dialect() {
              registerFunction("group_concat", new StandardSQLFunction("group_concat", StandardBasicTypes.STRING));
          }
      }
      ~~~

### JPQL 경로표현식
- 점(.)을 찍어 객체 그래프를 탐색하는 것
~~~sql
select m.username //상태필드
  from Member m 
    join m.team t // 단일값 연관 필드
    join m.orders o // 컬렉션값 연관 필드
where t.name = '팀A'
~~~
- 상태필드, 단일값 연관 필드, 컬렉션값 연관 필드에 따라 결과값이 달라지므로 꼭 구분해서 사용해야 한다.
- **상태 필드(state field)**
  - 단순히 값을 저장하기 위한 필드
  - `m.username`
- **연관 필드(association field)**
  - 연관관계를 위한 필드
  - **단일 값 연관 필드**
    - `@ManyToOne`
    - `@OneToOne`
    - 대상이 Entity (`m.team`)
  - **컬렉션 값 연관 필드**
    - `@OneToMany`
    - `@ManyToMany`
    - 대상이 컬렉션(`m.orders`)

### JPQL 경로표현식 특징
- 상태 필드(state field)
  - 경로 탐색의 끝
  - 더 이상 탐색 불가능
- 단일 값 연관 경로
  - **묵시적 내부 조인(inner join)** 발생
  - 탐색 가능
- 컬렉션 값 연관 경로
  - 묵시적 내부 조인 발생
  - 더 이상 탐색 불가능
  - From 절에서 명시적 조인을 통해 별칭을 얻으면, 별칭을 통해 탐색 가능

>실무에서는 묵시적 조인을 되도록 쓰지 말고, 명시적 조인을 쓰는 것이 좋다!
- 명시적 조인
  - join 키워드 직접 사용
  - `SELECT m FROM Member m JOIN m.team t`
- 묵시적 조인
  - 경로표현식에 의해 묵시적으로 SQL조인 발생 (내부 조인만 가능)
  - `SELECT m.team FROM Member m`
  
### 경로표현식 예제
- 성공
  - `SELECT o.member.team FROM Order o`
  - `SELECT t.members.username FROM Team t`
  - `SELECT m.username FROM Team t JOIN t.members m`
- 실패
  - `SELECT t.members.username FROM Team t`