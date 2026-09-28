package com.nutrihealth.auth.adapter.out.security;

import com.nutrihealth.auth.domain.model.Role;
import com.nutrihealth.auth.domain.model.UserAccount;
import com.nutrihealth.auth.domain.port.out.BiometricTokenVerifierPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local-dev / test stub for biometric verification.
 *
 * <p>Active when the {@code local-dev} Spring profile is set. Trusts any
 * non-blank biometric token for the given userId, constructing a synthetic
 * {@link UserAccount} so the rest of the flow (token issuance) works end-to-end
 * without a real device or GCP services.
 *
 * <p>This bean is <strong>never</strong> active in {@code staging} or
 * {@code prod} profiles.
 */
@Component
@Profile("local-dev")
public class LocalDevBiometricVerifierAdapter implements BiometricTokenVerifierPort {

    private static final Logger log = LoggerFactory.getLogger(LocalDevBiometricVerifierAdapter.class);

    @Override
    public UserAccount verify(String userId, String biometricToken) {
        log.warn("LOCAL-DEV biometric verifier — accepting any token for userId={}. "
                + "Do NOT use in production!", userId);
        // Return a synthetic CUSTOMER account — no database lookup needed in dev.
        return new UserAccount(UUID.fromString(userId), "dev@local.test", "NOT_A_HASH",
                Role.CUSTOMER, true);
    }
}
