package com.nutrihealth.diagnostic.adapter.out.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PhiFieldEncryptorTest {

    private final String key = Base64.getEncoder().encodeToString(new byte[32]);
    private final PhiFieldEncryptor encryptor = new PhiFieldEncryptor(key);

    @Test
    void decryptReversesEncrypt() {
        String plaintext = "{\"weightKg\":78.5,\"skinConditions\":[\"OILY_SKIN\"]}";

        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted).doesNotContain("weightKg", "OILY_SKIN");
    }

    @Test
    void encryptUsesRandomIvSoCiphertextIsNotDeterministic() {
        String plaintext = "same PHI value";

        Set<String> ciphertexts = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            ciphertexts.add(encryptor.encrypt(plaintext));
        }

        assertThat(ciphertexts).hasSize(5);
    }

    @Test
    void rejectsKeyThatIsNotExactly32Bytes() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PhiFieldEncryptor(shortKey));
    }
}
