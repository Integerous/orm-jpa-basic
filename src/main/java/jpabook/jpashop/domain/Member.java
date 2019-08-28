package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Member extends BaseEntity{

    @Id
    @GeneratedValue
    @Column(name = "MEMBER_ID")
    private Long id;
    private String name;
    private String city;
    private String street;
    private String zipcode;

    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>(); // Member가 orders를 가지고 있는 것은 좋은 설계가 아니다.
    // Order에 이미 memberId가 FK로 있기 때문에
    // 고객의 주문 내역을 조회한다면 Order에서 memberId 가지고 조회하는것이 더 깔끔하다.
}
