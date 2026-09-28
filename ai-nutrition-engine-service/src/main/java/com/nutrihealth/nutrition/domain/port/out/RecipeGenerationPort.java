package com.nutrihealth.nutrition.domain.port.out;

import com.nutrihealth.nutrition.domain.model.Recipe;
import java.util.Set;

/**
 * Outbound port to the generative AI model (Gemini, per the spec) used when
 * no existing recipe satisfies the caller's required tags without repeating
 * a recently served dish.
 */
public interface RecipeGenerationPort {

    Recipe generateRecipe(int targetCalories, Set<String> requiredSkinBenefitTags);
}
