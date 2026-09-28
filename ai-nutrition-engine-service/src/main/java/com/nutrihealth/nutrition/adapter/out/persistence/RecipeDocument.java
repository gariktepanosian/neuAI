package com.nutrihealth.nutrition.adapter.out.persistence;

import java.util.List;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Maps directly onto the spec's `AI_personalized_recipes` MongoDB collection shape. */
@Document(collection = "AI_personalized_recipes")
public class RecipeDocument {

    @Id
    private String recipeId;

    private String title;
    private MacrosEmbeddable macros;
    private List<String> micronutrientHighlights;
    private List<IngredientEmbeddable> ingredients;

    @Indexed
    private Set<String> targetSkinBenefits;

    private boolean isAntiMonotonyAlternative;

    public RecipeDocument() {
    }

    public RecipeDocument(String recipeId, String title, MacrosEmbeddable macros,
                           List<String> micronutrientHighlights, List<IngredientEmbeddable> ingredients,
                           Set<String> targetSkinBenefits, boolean isAntiMonotonyAlternative) {
        this.recipeId = recipeId;
        this.title = title;
        this.macros = macros;
        this.micronutrientHighlights = micronutrientHighlights;
        this.ingredients = ingredients;
        this.targetSkinBenefits = targetSkinBenefits;
        this.isAntiMonotonyAlternative = isAntiMonotonyAlternative;
    }

    public record MacrosEmbeddable(int calories, double proteinGrams, double carbsGrams, double fatGrams) {
    }

    public record IngredientEmbeddable(String item, double grams) {
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getTitle() {
        return title;
    }

    public MacrosEmbeddable getMacros() {
        return macros;
    }

    public List<String> getMicronutrientHighlights() {
        return micronutrientHighlights;
    }

    public List<IngredientEmbeddable> getIngredients() {
        return ingredients;
    }

    public Set<String> getTargetSkinBenefits() {
        return targetSkinBenefits;
    }

    public boolean isAntiMonotonyAlternative() {
        return isAntiMonotonyAlternative;
    }
}
