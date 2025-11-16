package ee.vaplaah.tic_tac_toe.utils.base;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEntity {

    @CreatedDate
    private LocalDateTime createdAt;
//    @CreatedBy
//    private String createdBy; // TODO: use User type https://docs.spring.io/spring-data/jpa/reference/auditing.html#auditing.reactive-auditor-aware
    @LastModifiedDate
    private LocalDateTime updatedAt;
//    @LastModifiedBy
//    private String updatedBy; // TODO: use User type
}