package com.nutrihealth.auth.adapter.out.security;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically refreshes the JWT signing key from GCP Secret Manager, enabling
 * zero-downtime key rotation without a service restart.
 *
 * <p>Design:
 * <ul>
 *   <li>At startup and every {@code KEY_ROTATION_INTERVAL_MS} (default 1 hour),
 *       this component fetches the <em>latest</em> version of the
 *       {@code jwt-signing-key-<env>} secret from GCP Secret Manager.</li>
 *   <li>The active key is stored in an {@link AtomicReference} so reads in
 *       {@link JwtTokenIssuerAdapter} and {@link com.nutrihealth.auth.adapter.in.web.security.JwtAuthenticationFilter}
 *       never block and always see the latest value.</li>
 *   <li>Both the issuer and the filter obtain the key through {@link #currentKey()},
 *       ensuring that signing and verification always use the same key version.</li>
 * </ul>
 *
 * <p>Fallback: when the service starts in local-dev mode (profile {@code local-dev})
 * or the Secret Manager is unreachable, the component falls back to the
 * {@code nutrihealth.auth.jwt-signing-key} property value so development requires
 * no GCP credentials.
 */
@Component
public class JwtSigningKeyRotationManager {

    private static final Logger log = LoggerFactory.getLogger(JwtSigningKeyRotationManager.class);
    private static final long KEY_ROTATION_INTERVAL_MS = 60 * 60 * 1000L; // 1 hour

    private final AtomicReference<SecretKey> currentKey = new AtomicReference<>();
    private final String projectId;
    private final String environment;
    private final String fallbackBase64Key;

    public JwtSigningKeyRotationManager(
            @Value("${GCP_PROJECT_ID:local-dev-project}") String projectId,
            @Value("${nutrihealth.environment:dev}") String environment,
            @Value("${nutrihealth.auth.jwt-signing-key}") String fallbackBase64Key) {
        this.projectId = projectId;
        this.environment = environment;
        this.fallbackBase64Key = fallbackBase64Key;
        // Perform an immediate rotation at startup so the key is never null.
        rotateKey();
    }

    /**
     * Returns the currently active signing/verification key.
     * Callers should call this on every request — never cache the result.
     */
    public SecretKey currentKey() {
        return currentKey.get();
    }

    /**
     * Scheduled rotation — runs every hour.
     * The fixed-delay ensures the next rotation starts 1 hour after the
     * previous one completes, not 1 hour after startup.
     */
    @Scheduled(fixedDelay = KEY_ROTATION_INTERVAL_MS, initialDelay = KEY_ROTATION_INTERVAL_MS)
    public void rotateKey() {
        if ("local-dev-project".equals(projectId)) {
            log.debug("Local-dev mode: using static JWT signing key, skipping Secret Manager fetch.");
            currentKey.compareAndSet(null, buildKey(fallbackBase64Key));
            return;
        }

        String secretName = "jwt-signing-key-" + environment;
        SecretVersionName versionName = SecretVersionName.of(projectId, secretName, "latest");

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            AccessSecretVersionResponse response = client.accessSecretVersion(versionName);
            String base64Key = response.getPayload().getData().toStringUtf8().trim();
            SecretKey newKey = buildKey(base64Key);
            SecretKey old = currentKey.getAndSet(newKey);
            if (old == null) {
                log.info("JWT signing key loaded from Secret Manager (secret={})", secretName);
            } else {
                log.info("JWT signing key rotated from Secret Manager (secret={})", secretName);
            }
        } catch (Exception ex) {
            log.error("Failed to fetch JWT signing key from Secret Manager (secret={}), "
                    + "retaining current key: {}", secretName, ex.getMessage());
            // If the key was never set (first startup and SM is unreachable), fall back
            // to the config property so the service still starts.
            currentKey.compareAndSet(null, buildKey(fallbackBase64Key));
        }
    }

    private static SecretKey buildKey(String base64Key) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
    }
}
