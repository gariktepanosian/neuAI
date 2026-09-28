package com.nutrihealth.nutrition.domain.model;

import java.util.List;
import java.util.Set;

/**
 * Core domain aggregate mirroring the spec's `AI_personalized_recipes`
 * MongoDB collection.
 */
public class Recipe {

    private final String recipeId;
    private final String title;
    private final Macros macros;
    private final List<String> micronutrientHighlights;
    private final List<Ingredient> ingredients;
    private final Set<String> targetSkinBenefits;
    private final boolean antiMonotonyAlternative;

    public Recipe(String recipeId, String title, Macros macros, List<String> micronutrientHighlights,
                   List<Ingredient> ingredients, Set<String> targetSkinBenefits, boolean antiMonotonyAlternative) {
        this.recipeId = recipeId;
        this.title = title;
        this.macros = macros;
        this.micronutrientHighlights = micronutrientHighlights;
        this.ingredients = ingredients;
        this.targetSkinBenefits = targetSkinBenefits;
        this.antiMonotonyAlternative = antiMonotonyAlternative;
    }

    public boolean matchesRequiredTags(Set<String> requiredSkinBenefitTags) {
        return targetSkinBenefits.containsAll(requiredSkinBenefitTags);
    }

    public Recipe withMacrosScaledTo(int targetCalories) {
        return new Recipe(recipeId, title, macros.scaleToCalories(targetCalories),
                micronutrientHighlights, ingredients, targetSkinBenefits, antiMonotonyAlternative);
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getTitle() {
        return title;
    }

    public Macros getMacros() {
        return macros;
    }

    public List<String> getMicronutrientHighlights() {
        return micronutrientHighlights;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public Set<String> getTargetSkinBenefits() {
        return targetSkinBenefits;
    }

    public boolean isAntiMonotonyAlternative() {
        return antiMonotonyAlternative;
    }

    public record Ingredient(String item, double grams) {
    }
}
