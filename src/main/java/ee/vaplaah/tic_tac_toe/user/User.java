package ee.vaplaah.tic_tac_toe.user;

import ee.vaplaah.tic_tac_toe.utils.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@Document
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    private String id;
    private String email;
    private String firstname;
    private String lastname;
}
