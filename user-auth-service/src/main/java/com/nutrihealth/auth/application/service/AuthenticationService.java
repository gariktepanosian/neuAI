package com.nutrihealth.auth.application.service;

import com.nutrihealth.auth.domain.model.EmailAlreadyRegisteredException;
import com.nutrihealth.auth.domain.model.InvalidCredentialsException;
import com.nutrihealth.auth.domain.model.Role;
import com.nutrihealth.auth.domain.model.UserAccount;
import com.nutrihealth.auth.domain.port.in.AuthenticationUseCase;
import com.nutrihealth.auth.domain.port.out.PasswordHasherPort;
import com.nutrihealth.auth.domain.port.out.TokenIssuerPort;
import com.nutrihealth.auth.domain.port.out.UserAccountRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements AuthenticationUseCase {

    private final UserAccountRepositoryPort repository;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;

    public AuthenticationService(UserAccountRepositoryPort repository,
                                  PasswordHasherPort passwordHasher,
                                  TokenIssuerPort tokenIssuer) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public RegisteredUser register(String email, String rawPassword) {
        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        UserAccount account = new UserAccount(
                UUID.randomUUID(), email, passwordHasher.hash(rawPassword), Role.CUSTOMER, true);
        UserAccount saved = repository.save(account);
        return new RegisteredUser(saved.getId().toString(), saved.getEmail());
    }

    @Override
    public IssuedToken login(String email, String rawPassword) {
        UserAccount account = repository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!account.isActive() || !passwordHasher.matches(rawPassword, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        TokenIssuerPort.IssuedAccessToken issued = tokenIssuer.issueFor(account);
        return new IssuedToken(issued.token(), issued.expiresAtEpochSeconds());
    }
}
