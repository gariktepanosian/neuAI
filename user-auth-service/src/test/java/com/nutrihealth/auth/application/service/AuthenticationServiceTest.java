package com.nutrihealth.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutrihealth.auth.adapter.out.security.BCryptPasswordHasherAdapter;
import com.nutrihealth.auth.domain.model.EmailAlreadyRegisteredException;
import com.nutrihealth.auth.domain.model.InvalidCredentialsException;
import com.nutrihealth.auth.domain.model.UserAccount;
import com.nutrihealth.auth.domain.port.out.TokenIssuerPort;
import com.nutrihealth.auth.domain.port.out.UserAccountRepositoryPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {

    private final Map<String, UserAccount> store = new HashMap<>();
    private final UserAccountRepositoryPort repository = new UserAccountRepositoryPort() {
        @Override
        public UserAccount save(UserAccount account) {
            store.put(account.getEmail(), account);
            return account;
        }

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return Optional.ofNullable(store.get(email));
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.containsKey(email);
        }
    };

    private final BCryptPasswordHasherAdapter passwordHasher = new BCryptPasswordHasherAdapter();
    private final TokenIssuerPort tokenIssuer = account ->
            new TokenIssuerPort.IssuedAccessToken("fake-token-for-" + account.getEmail(), 9_999_999_999L);

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(repository, passwordHasher, tokenIssuer);
    }

    @Test
    void registerThenLoginSucceedsWithCorrectPassword() {
        service.register("user@example.com", "correct-horse-battery-staple");

        var token = service.login("user@example.com", "correct-horse-battery-staple");

        assertThat(token.accessToken()).isEqualTo("fake-token-for-user@example.com");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        service.register("user@example.com", "correct-horse-battery-staple");

        assertThatThrownBy(() -> service.register("user@example.com", "another-password"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        service.register("user@example.com", "correct-horse-battery-staple");

        assertThatThrownBy(() -> service.login("user@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsUnknownEmail() {
        assertThatThrownBy(() -> service.login("nobody@example.com", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
