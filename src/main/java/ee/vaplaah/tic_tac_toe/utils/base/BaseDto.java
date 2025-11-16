package ee.vaplaah.tic_tac_toe.utils.base;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseDto {

    private LocalDateTime createdAt;
//    @CreatedBy
//    private String createdBy; // TODO: use User type https://docs.spring.io/spring-data/jpa/reference/auditing.html#auditing.reactive-auditor-aware
    private LocalDateTime updatedAt;
//    @LastModifiedBy
//    private String updatedBy; // TODO: use User type
}