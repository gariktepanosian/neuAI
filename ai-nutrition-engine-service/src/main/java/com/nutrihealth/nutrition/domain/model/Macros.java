package com.nutrihealth.nutrition.domain.model;

/**
 * Macronutrient breakdown for a recipe, in grams (except calories). Immutable
 * value object so scaling always produces a new instance.
 */
public record Macros(int calories, double proteinGrams, double carbsGrams, double fatGrams) {

    public Macros {
        if (calories < 0 || proteinGrams < 0 || carbsGrams < 0 || fatGrams < 0) {
            throw new IllegalArgumentException("Macro values cannot be negative");
        }
    }

    /**
     * Scales every macro proportionally so the recipe's calorie count matches
     * {@code targetCalories}, preserving the original macro ratio — the
     * "macro scaling" behavior called out in the roadmap for adapting a
     * recipe to an individual's calorie needs.
     */
    public Macros scaleToCalories(int targetCalories) {
        if (targetCalories <= 0) {
            throw new IllegalArgumentException("targetCalories must be positive");
        }
        if (calories == 0) {
            throw new IllegalStateException("Cannot scale a recipe with zero base calories");
        }

        double ratio = (double) targetCalories / calories;
        return new Macros(targetCalories, proteinGrams * ratio, carbsGrams * ratio, fatGrams * ratio);
    }
}
