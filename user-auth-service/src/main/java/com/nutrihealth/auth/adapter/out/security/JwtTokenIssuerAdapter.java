package com.nutrihealth.auth.adapter.out.security;

import com.nutrihealth.auth.domain.model.UserAccount;
import com.nutrihealth.auth.domain.port.out.TokenIssuerPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues signed JWT access tokens (HS256). The signing key is expected to be
 * rotated periodically via GCP Secret Manager in deployed environments
 * (injected through {@code nutrihealth.auth.jwt-signing-key}); this adapter
 * only knows how to use "the current key", not how to rotate it.
 */
@Component
public class JwtTokenIssuerAdapter implements TokenIssuerPort {

    private final SecretKey signingKey;
    private final Duration tokenTtl;

    public JwtTokenIssuerAdapter(@Value("${nutrihealth.auth.jwt-signing-key}") String base64Key,
                                  @Value("${nutrihealth.auth.jwt-ttl-minutes:60}") long ttlMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
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
                .signWith(signingKey)
                .compact();

        return new IssuedAccessToken(token, expiresAt.getEpochSecond());
    }
}
