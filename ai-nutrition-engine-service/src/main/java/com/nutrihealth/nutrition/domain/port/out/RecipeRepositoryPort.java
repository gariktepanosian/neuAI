package com.nutrihealth.nutrition.domain.port.out;

import com.nutrihealth.nutrition.domain.model.Recipe;
import java.util.List;
import java.util.Set;

public interface RecipeRepositoryPort {

    Recipe save(Recipe recipe);

    /** Candidates matching all required tags, for the caller to filter against recently-served ids. */
    List<Recipe> findByTargetSkinBenefitsContainingAll(Set<String> requiredSkinBenefitTags);
}
