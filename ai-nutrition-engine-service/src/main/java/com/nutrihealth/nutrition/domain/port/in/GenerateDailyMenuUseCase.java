package com.nutrihealth.nutrition.domain.port.in;

import com.nutrihealth.nutrition.domain.model.Recipe;
import java.util.Set;

public interface GenerateDailyMenuUseCase {

    /**
     * Selects (or, failing that, generates) a recipe matching the caller's
     * biomarker-derived dietary tags, scaled to their calorie target, while
     * avoiding recently served recipes ("anti-monotony" rotation).
     *
     * @param requiredSkinBenefitTags e.g. derived from a LOW vitamin D FHIR
     *                                observation via the clinic-diagnostic-service
     * @param recentRecipeIds         recipes already served this week, to exclude
     */
    Recipe recommendRecipe(int targetCalories, Set<String> requiredSkinBenefitTags, Set<String> recentRecipeIds);
}
