package ee.vaplaah.gomoku.configuration;

import ee.vaplaah.gomoku.core.base.BaseEntity;
import ee.vaplaah.gomoku.user.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Enables reactive MongoDB auditing and provides the two beans that Spring Data needs to
 * automatically populate {@code @CreatedBy} / {@code @LastModifiedBy} and
 * {@code @CreatedDate} / {@code @LastModifiedDate} fields on document saves.
 *
 * <p>Spring Data MongoDB's auditing infrastructure requires
 * two provider beans: a {@link ReactiveAuditorAware} to resolve the current actor and a
 * {@link DateTimeProvider} for timestamp normalization.
 * Without this configuration class, audit fields would never be populated, silently producing
 * {@code null} values. Using {@code @EnableReactiveMongoAuditing} here (rather than on the
 * main application class) keeps auditing concerns grouped with other infrastructure config.
 * </p>
 *
 * <p>{@code @EnableReactiveMongoAuditing} activates Spring Data's reactive auditing interceptor.
 * The {@code auditor()} bean is invoked by Spring Data on every MongoDB save / update operation
 * that targets a document extending {@link BaseEntity}.
 * It reads the current principal from {@code ReactiveSecurityContextHolder}.
 * If the context is empty (unauthenticated request) or the principal is not
 * authenticated, the {@code filter(Authentication::isAuthenticated)} guard causes the
 * {@code Mono} to complete empty, resulting in no auditor being set.
 * </p>
 */
@Configuration
@EnableReactiveMongoAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class AuditConfiguration {

    @Bean
    public ReactiveAuditorAware<String> auditor() {
        return () -> ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getPrincipal)
            .cast(User.class)
            .map(User::getId);
    }

    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ZoneOffset.UTC));
    }
}
