package com.nutrihealth.auth.adapter.in.web.security;

import com.nutrihealth.auth.adapter.out.security.JwtSigningKeyRotationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the {@code Authorization: Bearer <jwt>} header on incoming
 * requests and populates the Spring Security context with the caller's role.
 *
 * <p>The signing key is obtained from {@link JwtSigningKeyRotationManager} on
 * every call, so key rotations (hourly, via GCP Secret Manager) are transparent
 * to in-flight requests — new tokens are issued with the new key while old tokens
 * remain valid until their {@code exp} claim expires.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtSigningKeyRotationManager keyManager;

    public JwtAuthenticationFilter(JwtSigningKeyRotationManager keyManager) {
        this.keyManager = keyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(keyManager.currentKey())
                        .build()
                        .parseSignedClaims(header.substring(7))
                        .getPayload();

                String role = claims.get("role", String.class);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
