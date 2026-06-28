package ee.vaplaah.tic_tac_toe.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ee.vaplaah.tic_tac_toe.core.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Document
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
public class User extends BaseEntity implements UserDetails {

    @ToString.Include
    @Indexed(unique = true)
    private String username;
    @JsonIgnore
    private String password;
    @ToString.Include
    private List<String> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // Populated in UserDetailsService
    }
}
