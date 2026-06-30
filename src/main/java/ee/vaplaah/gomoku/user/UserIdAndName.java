package ee.vaplaah.gomoku.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdAndName {

    @Indexed
    private String userId;
    private String username;

    public static UserIdAndName fromUser(User user) {
        return UserIdAndName.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }
}
