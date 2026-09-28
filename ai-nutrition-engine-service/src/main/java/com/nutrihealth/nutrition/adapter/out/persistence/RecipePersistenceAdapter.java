package com.nutrihealth.nutrition.adapter.out.persistence;

import com.nutrihealth.nutrition.domain.model.Macros;
import com.nutrihealth.nutrition.domain.model.Recipe;
import com.nutrihealth.nutrition.domain.port.out.RecipeRepositoryPort;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RecipePersistenceAdapter implements RecipeRepositoryPort {

    private final SpringDataRecipeRepository springDataRepository;

    public RecipePersistenceAdapter(SpringDataRecipeRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Recipe save(Recipe recipe) {
        RecipeDocument document = toDocument(recipe);
        springDataRepository.save(document);
        return recipe;
    }

    @Override
    public List<Recipe> findByTargetSkinBenefitsContainingAll(Set<String> requiredSkinBenefitTags) {
        return springDataRepository.findByTargetSkinBenefitsContainingAll(requiredSkinBenefitTags).stream()
                .map(this::toDomain)
                .toList();
    }

    private RecipeDocument toDocument(Recipe recipe) {
        Macros macros = recipe.getMacros();
        List<RecipeDocument.IngredientEmbeddable> ingredients = recipe.getIngredients().stream()
                .map(i -> new RecipeDocument.IngredientEmbeddable(i.item(), i.grams()))
                .toList();

        return new RecipeDocument(recipe.getRecipeId(), recipe.getTitle(),
                new RecipeDocument.MacrosEmbeddable(macros.calories(), macros.proteinGrams(),
                        macros.carbsGrams(), macros.fatGrams()),
                recipe.getMicronutrientHighlights(), ingredients, recipe.getTargetSkinBenefits(),
                recipe.isAntiMonotonyAlternative());
    }

    private Recipe toDomain(RecipeDocument document) {
        RecipeDocument.MacrosEmbeddable m = document.getMacros();
        Macros macros = new Macros(m.calories(), m.proteinGrams(), m.carbsGrams(), m.fatGrams());
        List<Recipe.Ingredient> ingredients = document.getIngredients().stream()
                .map(i -> new Recipe.Ingredient(i.item(), i.grams()))
                .toList();

        return new Recipe(document.getRecipeId(), document.getTitle(), macros,
                document.getMicronutrientHighlights(), ingredients, document.getTargetSkinBenefits(),
                document.isAntiMonotonyAlternative());
    }
}
