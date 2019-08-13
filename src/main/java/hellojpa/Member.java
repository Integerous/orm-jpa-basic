package hellojpa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
//@Table(name = "USER") //쿼리가 USER라는 테이블에 나간다
//@Table(uniqueConstraints = "") //컬럼이 아니라 여기서 유니크 제약조건을 주면 이름까지 설정할 수 있다.
@Getter
@Setter
@NoArgsConstructor // JPA는 내부적으로 리플렉션이 일어나기 때문에 동적으로 객체를 생성해야 한다. 그래서 기본생성자가 필요하다.
@AllArgsConstructor
public class Member {

    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO) // DB 방언에 맞추어 아래 셋 중에 하나 선택
//    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본키 생성을 DB에 위임.
//    @GeneratedValue(strategy = GenerationType.SEQUENCE) // 데이터베이스 시퀀스 오브젝트 사용
    @GeneratedValue(strategy = GenerationType.TABLE) // 키 생성 전용 테이블을 하나 만들어서 DB 시퀀스를 흉내내는 전략
    private Long id;

//    @Column(name = "name") // 객체는 username이라 쓰고, DB에는 name이라고 쓸 때
//    @Column(updatable = false) // 이 컬럼은 절대 변경되지 않는다.(DB에서 강제로 업데이트 하지 않는 이상)
//    @Column(nullable = false) // Not null 제약조건을 걸어준다.
//    @Column(unique = true) // 얘는 여기서 잘 안쓴다. 유니크 제약조건을 만들어주기는 하는데, 이름이 랜덤하게 설정되고, 한 컬럼에만 적용되고 복합에서는 안되기 때문에 운영에서 사용하기 어렵다. 때문에 위의 @Table에서 사용한다.
    @Column(columnDefinition = "varchar(100) default 'EMPTY'") // 특정 DB에 종속적인 옵션들을 직접 넣을 수 있다.
    private String username;

    @Column()
    private Integer age; // Integer를 써도 생각한 대로 DB에 Integer와 가장 적절한 숫자 타입이 설정된다.

    @Enumerated(EnumType.STRING) // DB에는 Enum 타입이 없어서 @Enumerated 추가해야 한다.
    private RoleType roleType;

    @Temporal(TemporalType.TIMESTAMP) //DATE, TIME, TIMESTAMP 세가지가 있는데, 보통 DB는 이 세가지를 구분해서 쓰므로 매핑정보를 줘야 한다.
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDate;

    @Lob // DB에 varchar를 넘어서는 큰 컨텐츠를 넣고 싶을 경우 @Lob 사용
    private String description;

    @Transient // DB랑 관계없이 메모리에서만 계산하고 싶을 때, 즉, DB의 컬럼과 매핑하지 않을 때 사용
    private int temp;

    private LocalDate testLocalDate; //하이버네이트 최신버전에서는 @Temporal 없이 이렇게 쓰면 된다.
    private LocalDateTime testLocalDateTime;
}
