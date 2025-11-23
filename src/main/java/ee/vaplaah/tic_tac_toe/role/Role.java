package ee.vaplaah.tic_tac_toe.role;

import ee.vaplaah.tic_tac_toe.utils.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
@Builder
@Document
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity implements GrantedAuthority {

    @Id
    private String id;
    @Indexed(unique = true)
    private String name;

    @Override
    public String getAuthority() {
        return name;
    }
}
