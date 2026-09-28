package com.nutrihealth.nutrition.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class MacrosTest {

    @Test
    void scaleToCaloriesPreservesMacroRatio() {
        Macros base = new Macros(650, 48.0, 52.0, 22.0);

        Macros scaled = base.scaleToCalories(1300);

        assertThat(scaled.calories()).isEqualTo(1300);
        assertThat(scaled.proteinGrams()).isCloseTo(96.0, within(0.001));
        assertThat(scaled.carbsGrams()).isCloseTo(104.0, within(0.001));
        assertThat(scaled.fatGrams()).isCloseTo(44.0, within(0.001));
    }

    @Test
    void scaleDownHalvesMacros() {
        Macros base = new Macros(2000, 100.0, 200.0, 80.0);

        Macros scaled = base.scaleToCalories(1000);

        assertThat(scaled.proteinGrams()).isCloseTo(50.0, within(0.001));
        assertThat(scaled.carbsGrams()).isCloseTo(100.0, within(0.001));
        assertThat(scaled.fatGrams()).isCloseTo(40.0, within(0.001));
    }

    @Test
    void rejectsNonPositiveTargetCalories() {
        Macros base = new Macros(500, 10, 10, 10);

        assertThatThrownBy(() -> base.scaleToCalories(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsScalingARecipeWithZeroBaseCalories() {
        Macros base = new Macros(0, 0, 0, 0);

        assertThatThrownBy(() -> base.scaleToCalories(500)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNegativeMacroValues() {
        assertThatThrownBy(() -> new Macros(-1, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
