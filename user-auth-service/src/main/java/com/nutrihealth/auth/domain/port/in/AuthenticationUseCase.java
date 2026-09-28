package com.nutrihealth.auth.domain.port.in;

public interface AuthenticationUseCase {

    RegisteredUser register(String email, String rawPassword);

    IssuedToken login(String email, String rawPassword);

    record RegisteredUser(String userId, String email) {
    }

    record IssuedToken(String accessToken, long expiresAtEpochSeconds) {
    }
}
