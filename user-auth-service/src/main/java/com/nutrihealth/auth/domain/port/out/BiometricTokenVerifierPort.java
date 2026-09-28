package com.nutrihealth.auth.domain.port.out;

import com.nutrihealth.auth.domain.model.UserAccount;

/**
 * Outbound port for biometric authentication verification.
 *
 * <p>During biometric login, the mobile client sends a {@code biometricToken} that
 * encapsulates a device-signed challenge (e.g. Android Keystore / iOS SecureEnclave
 * attestation). This port decouples the use-case from the specific verifier
 * implementation, which in production calls the platform-specific attestation API.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code FidoAttestationBiometricVerifierAdapter} — production; calls Google
 *       Play Integrity / Apple DeviceCheck APIs to verify the hardware-backed token.</li>
 *   <li>{@code AlwaysTrueBiometricVerifierAdapter} — local-dev/test; always returns
 *       a stub {@link UserAccount} with the supplied userId, no network call.</li>
 * </ul>
 */
public interface BiometricTokenVerifierPort {

    /**
     * Verifies a biometric assertion token and returns the associated account.
     *
     * @param userId        the userId claim extracted from the client's biometric JWT
     * @param biometricToken the raw attestation token from the client device
     * @return the matching {@link UserAccount} when attestation succeeds
     * @throws BiometricVerificationException if the token is invalid, expired, or
     *         fails the hardware attestation check
     */
    UserAccount verify(String userId, String biometricToken);

    /** Thrown when biometric attestation fails for any reason. */
    class BiometricVerificationException extends RuntimeException {
        public BiometricVerificationException(String message) { super(message); }
        public BiometricVerificationException(String message, Throwable cause) { super(message, cause); }
    }
}
