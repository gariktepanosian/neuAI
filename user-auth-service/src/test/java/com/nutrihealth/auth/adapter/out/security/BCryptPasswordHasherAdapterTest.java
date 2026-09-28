package com.nutrihealth.auth.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherAdapterTest {

    private final BCryptPasswordHasherAdapter hasher = new BCryptPasswordHasherAdapter();

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hash = hasher.hash("correct-horse-battery-staple");

        assertThat(hasher.matches("correct-horse-battery-staple", hash)).isTrue();
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String hash = hasher.hash("correct-horse-battery-staple");

        assertThat(hasher.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void hashIsNotThePlaintextPassword() {
        String hash = hasher.hash("super-secret");

        assertThat(hash).isNotEqualTo("super-secret");
    }
}
