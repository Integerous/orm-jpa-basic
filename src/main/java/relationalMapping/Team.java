package relationalMapping;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Team {

    @Id
    @GeneratedValue
    @Column(name = "TEAM_ID")
    private Long id;

    private String name;

    @OneToMany(mappedBy = "team") // 연관관계 주인인 team에 의해 관리되므로 여기에 값을 넣거나 업데이트해도 소용없다. 다만 조회는 가능하다.
    private List<Member> members = new ArrayList<>(); // 이렇게 ArrayList로 초기화 하면 add 할 때 NPE를 방지할 수 있으므로 관례적으로 사용한다.
}
