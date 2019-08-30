package relationalMapping;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "MEMBER_ID")
    private Long id;

    @Column(name = "USERNAME")
    private String username;

//    @Column(name = "TEAM_ID")
//    private Long teamId;

    //기간(Period)
//    private LocalDateTime startDate;
//    private LocalDateTime endDate;
    @Embedded
    private Period workPeriod;

    //주소(Address)
//    private String city;
//    private String street;
//    private String zipcode;
    @Embedded
    private Address homeAddress;

    @Embedded// 한 엔티티에서 같은 값 타입을 사용하면 중복된 컬럼 매핑으로 에러가 나서 아래와 같이 설정한다.
    @AttributeOverrides({
            @AttributeOverride(name = "city",
                    column = @Column(name = "WORK_CITY")),
            @AttributeOverride(name = "street",
                    column = @Column(name = "WORK_STREET")),
            @AttributeOverride(name = "zipcode",
                    column = @Column(name = "WORK_ZIPCODE"))
    })
    private Address workAddress;




    @ManyToOne
    @JoinColumn(name = "TEAM_ID") // Member 객체의 team과 MEMBER 테이블의 TEAM_ID(FK)를 연관관계로 매핑한다는 뜻
    private Team team;

    @OneToOne
    @JoinColumn(name = "LOCKER_ID")
    private Locker locker;

//    @ManyToMany
//    @JoinTable(name = "MEMBER_PRODUCT")
//    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<MemberProduct> memberProducts = new ArrayList<>();

}