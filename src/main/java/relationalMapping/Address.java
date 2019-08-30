package relationalMapping;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Embeddable;

@Embeddable // 값타입을 정의
@Getter
@Setter
@NoArgsConstructor
public class Address {

    private String city;
    private String street;
    private String zipcode;
}
