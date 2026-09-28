package com.nutrihealth.auth.adapter.out.persistence;

import com.nutrihealth.auth.domain.model.UserAccount;
import com.nutrihealth.auth.domain.port.out.UserAccountRepositoryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserAccountPersistenceAdapter implements UserAccountRepositoryPort {

    private final SpringDataUserAccountRepository springDataRepository;

    public UserAccountPersistenceAdapter(SpringDataUserAccountRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public UserAccount save(UserAccount account) {
        UserAccountEntity entity = new UserAccountEntity(
                account.getId(), account.getEmail(), account.getPasswordHash(),
                account.getRole(), account.isActive());
        springDataRepository.save(entity);
        return account;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return springDataRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(entity.getId(), entity.getEmail(), entity.getPasswordHash(),
                entity.getRole(), entity.isActive());
    }
}
