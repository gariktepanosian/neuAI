package com.nutrihealth.auth.domain.model;

import java.util.UUID;

/**
 * Core domain entity for an authenticated principal. Deliberately holds only
 * identity/credential concerns (per "Database Per Service" + PHI isolation:
 * clinical and payment data live in their own services, not here).
 */
public class UserAccount {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    public UserAccount(UUID id, String email, String passwordHash, Role role, boolean active) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
