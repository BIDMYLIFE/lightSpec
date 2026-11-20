package com.lightspec.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialPolicyTests {

    private final CredentialPolicy policy = new CredentialPolicy();

    @Test
    void trimsAndCaseFoldsWithLocaleIndependentRules() {
        var normalized = policy.normalizeUsername("  AdMiN  ");

        assertThat(normalized.display()).isEqualTo("AdMiN");
        assertThat(normalized.canonical()).isEqualTo("admin");
    }

    @Test
    void normalizesUnicodeCompatibilityForms() {
        assertThat(policy.normalizeUsername("Ａｄｍｉｎ").canonical()).isEqualTo("admin");
        assertThat(policy.normalizeUsername("管理員").canonical()).isEqualTo("管理員");
    }

    @Test
    void rejectsInvalidUsernameCharactersAndLengths() {
        assertThatThrownBy(() -> policy.normalizeUsername("ab"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.normalizeUsername("name with spaces"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.normalizeUsername("a".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsPasswordBoundariesAndRejectsOutsideThem() {
        policy.validatePassword("12345678");
        policy.validatePassword("密".repeat(128));

        assertThatThrownBy(() -> policy.validatePassword("1234567"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validatePassword("x".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validatePassword("valid123\n"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
