package com.nutrihealth.auth.domain.port.in;

public interface AuthenticationUseCase {

    RegisteredUser register(String email, String rawPassword);

    IssuedToken login(String email, String rawPassword);

    /**
     * Authenticates a user via a hardware-backed biometric attestation token
     * (fingerprint / face ID from Android Keystore or iOS Secure Enclave).
     *
     * @param userId        UUID of the user as supplied by the mobile client
     * @param biometricToken device-signed JWT proving biometric success
     * @return a new platform access token on success
     */
    IssuedToken biometricLogin(String userId, String biometricToken);

    record RegisteredUser(String userId, String email) {
    }

    record IssuedToken(String accessToken, long expiresAtEpochSeconds) {
    }
}
