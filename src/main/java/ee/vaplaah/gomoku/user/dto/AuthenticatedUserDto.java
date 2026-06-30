package ee.vaplaah.gomoku.user.dto;

import ee.vaplaah.gomoku.core.base.BaseDto;
import ee.vaplaah.gomoku.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * DTO representing an authenticated user in response to any authentication-related requests.
 */
@Getter
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUserDto extends BaseDto {

    private String username;
    private List<String> roles;

    public static AuthenticatedUserDto from(User user) {
        return AuthenticatedUserDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .roles(user.getRoles())
            .build();
    }
}