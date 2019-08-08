package hellojpa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
//@Table(name = "USER") //쿼리가 USER라는 테이블에 나간다
@Getter
@Setter
@NoArgsConstructor // JPA는 내부적으로 리플렉션이 일어나기 때문에 동적으로 객체를 생성해야 한다. 그래서 기본생성자가 필요하다.
@AllArgsConstructor
public class Member {

    @Id
    private Long id;
    private String name;
}
