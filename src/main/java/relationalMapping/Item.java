package relationalMapping;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED) // 이렇게 잡으면 조인전략으로 매핑
@DiscriminatorColumn // DTYPE 라는 컬럼이 생성되고, ITEM 객체를 상속받은 엔티티 중 join된 엔티티의 이름이 들어가게 된다.
public abstract class Item {

    @Id @GeneratedValue
    private Long id;

    private String name;
    private int price;

}
