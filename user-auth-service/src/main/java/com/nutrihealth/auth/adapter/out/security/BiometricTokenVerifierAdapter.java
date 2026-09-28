package com.nutrihealth.auth.adapter.out.security;

import com.nutrihealth.auth.domain.model.Role;
import com.nutrihealth.auth.domain.model.UserAccount;
import com.nutrihealth.auth.domain.port.out.BiometricTokenVerifierPort;
import com.nutrihealth.auth.domain.port.out.UserAccountRepositoryPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production biometric token verifier.
 *
 * <p>The mobile app performs local biometric authentication (fingerprint / face)
 * and then signs a short-lived JWT with a private key stored in the device's
 * hardware-backed keystore (Android Keystore / iOS Secure Enclave). The public
 * key corresponding to the device is registered at enrolment time and stored
 * against the user account.
 *
 * <p>Verification steps:
 * <ol>
 *   <li>Parse the biometric JWT header to extract the {@code kid} (key-ID = userId).</li>
 *   <li>Load the registered device public-key material for that user from the DB.</li>
 *   <li>Verify the JWT signature with the loaded public key.</li>
 *   <li>Assert {@code sub} == userId and the token is not expired.</li>
 *   <li>Return the live {@link UserAccount} from the repository.</li>
 * </ol>
 *
 * <p>For the MVP we use a symmetric HMAC key shared with the device (seeded into the
 * device at enrolment via a QR code exchange), using the platform-wide
 * {@code JWT_SIGNING_KEY}.  Asymmetric per-device keys are the target architecture
 * and can be swapped in by implementing a {@code DevicePublicKeyRepositoryPort}.
 */
@Component
@Profile("!local-dev")
public class BiometricTokenVerifierAdapter implements BiometricTokenVerifierPort {

    private static final Logger log = LoggerFactory.getLogger(BiometricTokenVerifierAdapter.class);

    private final SecretKey verificationKey;
    private final UserAccountRepositoryPort userRepository;

    @Autowired
    public BiometricTokenVerifierAdapter(
            @Value("${nutrihealth.auth.jwt-signing-key}") String base64Key,
            UserAccountRepositoryPort userRepository) {
        this.verificationKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
        this.userRepository = userRepository;
    }

    @Override
    public UserAccount verify(String userId, String biometricToken) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(verificationKey)
                    .build()
                    .parseSignedClaims(biometricToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Biometric token verification failed for userId={}: {}", userId, ex.getMessage());
            throw new BiometricVerificationException("Invalid or expired biometric token", ex);
        }

        String tokenSubject = claims.getSubject();
        if (!userId.equals(tokenSubject)) {
            throw new BiometricVerificationException(
                    "Biometric token subject mismatch: expected " + userId + " got " + tokenSubject);
        }

        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BiometricVerificationException(
                        "User not found for biometric token subject: " + userId));
    }
}
