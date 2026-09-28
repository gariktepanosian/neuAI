package com.nutrihealth.auth.domain.port.out;

import com.nutrihealth.auth.domain.model.UserAccount;

public interface TokenIssuerPort {

    IssuedAccessToken issueFor(UserAccount account);

    record IssuedAccessToken(String token, long expiresAtEpochSeconds) {
    }
}
