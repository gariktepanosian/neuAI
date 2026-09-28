package com.nutrihealth.auth.domain.port.out;

import com.nutrihealth.auth.domain.model.UserAccount;
import java.util.Optional;

public interface UserAccountRepositoryPort {

    UserAccount save(UserAccount account);

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
