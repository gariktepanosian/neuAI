package com.nutrihealth.auth.adapter.out.security;

import com.nutrihealth.auth.domain.model.UserAccount;
import com.nutrihealth.auth.domain.port.out.TokenIssuerPort;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues signed JWT access tokens (HS256). The signing key is obtained from
 * {@link JwtSigningKeyRotationManager}, which refreshes it from GCP Secret Manager
 * every hour — enabling zero-downtime key rotation without a service restart.
 */
@Component
public class JwtTokenIssuerAdapter implements TokenIssuerPort {

    private final JwtSigningKeyRotationManager keyManager;
    private final Duration tokenTtl;

    public JwtTokenIssuerAdapter(JwtSigningKeyRotationManager keyManager,
                                  @Value("${nutrihealth.auth.jwt-ttl-minutes:60}") long ttlMinutes) {
        this.keyManager = keyManager;
        this.tokenTtl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public IssuedAccessToken issueFor(UserAccount account) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenTtl);

        String token = Jwts.builder()
                .subject(account.getId().toString())
                .claim("email", account.getEmail())
                .claim("role", account.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(keyManager.currentKey())
                .compact();

        return new IssuedAccessToken(token, expiresAt.getEpochSecond());
    }
}
