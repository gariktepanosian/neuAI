package com.nutrihealth.nutrition.adapter.in.web;

import com.nutrihealth.nutrition.domain.model.Macros;
import com.nutrihealth.nutrition.domain.model.Recipe;
import java.util.List;
import java.util.Set;

public record RecipeResponse(String recipeId, String title, Macros macros,
                              List<String> micronutrientHighlights, Set<String> targetSkinBenefits) {

    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(recipe.getRecipeId(), recipe.getTitle(), recipe.getMacros(),
                recipe.getMicronutrientHighlights(), recipe.getTargetSkinBenefits());
    }
}
