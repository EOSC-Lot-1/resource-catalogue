package gr.uoa.di.madgik.resourcecatalogue.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Test-only security configuration. Provides a stub {@link ClientRegistrationRepository}
 * so that Spring Boot's OAuth2 client auto-configuration backs off (it is
 * {@code @ConditionalOnMissingBean}). This prevents startup failures caused by
 * the empty {@code client-id} in the bundled {@code application.properties}
 * when running tests with the {@code no-auth} profile.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration stub = ClientRegistration
                .withRegistrationId("eosc")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/eosc")
                .authorizationUri("http://localhost/oauth2/authorize")
                .tokenUri("http://localhost/oauth2/token")
                .scope("openid", "email", "profile")
                .build();
        return new InMemoryClientRegistrationRepository(stub);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder() {
        return token -> {
            throw new BadJwtException("Test environment – JWT decoding not supported");
        };
    }
}
