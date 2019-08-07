package hellojpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
//@Table(name = "USER") //쿼리가 USER라는 테이블에 나간다
@Getter
@Setter
public class Member {

    @Id
    private Long id;
    private String name;
}
