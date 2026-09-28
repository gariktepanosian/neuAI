package com.nutrihealth.auth.domain.port.out;

import com.nutrihealth.auth.domain.model.UserAccount;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepositoryPort {

    UserAccount save(UserAccount account);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UUID id);

    boolean existsByEmail(String email);
}
