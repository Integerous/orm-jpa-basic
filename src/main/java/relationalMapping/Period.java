package relationalMapping;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable // 값타입을 정의
@Getter
@Setter
@NoArgsConstructor
public class Period {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
