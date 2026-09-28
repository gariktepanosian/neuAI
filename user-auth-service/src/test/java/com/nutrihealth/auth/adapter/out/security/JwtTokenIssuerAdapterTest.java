package com.nutrihealth.auth.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutrihealth.auth.domain.model.Role;
import com.nutrihealth.auth.domain.model.UserAccount;
import com.nutrihealth.auth.domain.port.out.TokenIssuerPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenIssuerAdapterTest {

    private final String base64Key = Base64.getEncoder().encodeToString(new byte[32]);
    private final JwtTokenIssuerAdapter issuer = new JwtTokenIssuerAdapter(base64Key, 60);

    @Test
    void issuedTokenEncodesUserIdEmailAndRole() {
        UserAccount account = new UserAccount(
                UUID.randomUUID(), "user@example.com", "hash", Role.CUSTOMER, true);

        TokenIssuerPort.IssuedAccessToken issued = issuer.issueFor(account);

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(issued.token()).getPayload();

        assertThat(claims.getSubject()).isEqualTo(account.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(issued.expiresAtEpochSeconds()).isGreaterThan(Instant.now().getEpochSecond());
    }
}
