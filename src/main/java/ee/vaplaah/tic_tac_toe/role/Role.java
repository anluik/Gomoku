package ee.vaplaah.tic_tac_toe.role;

import ee.vaplaah.tic_tac_toe.utils.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;

@Data
@Document
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity implements GrantedAuthority {

    @Indexed(unique = true)
    private String name;

    @Override
    public String getAuthority() {
        if (name == null) return null;

        if (name.startsWith("ROLE_")) {
            return name;
        }
        return "ROLE_" + name;
    }
}