package com.nutrihealth.nutrition.application.service;

import com.nutrihealth.nutrition.domain.model.Recipe;
import com.nutrihealth.nutrition.domain.port.in.GenerateDailyMenuUseCase;
import com.nutrihealth.nutrition.domain.port.out.RecipeGenerationPort;
import com.nutrihealth.nutrition.domain.port.out.RecipeRepositoryPort;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MenuRecommendationService implements GenerateDailyMenuUseCase {

    private final RecipeRepositoryPort repository;
    private final RecipeGenerationPort recipeGenerator;

    public MenuRecommendationService(RecipeRepositoryPort repository, RecipeGenerationPort recipeGenerator) {
        this.repository = repository;
        this.recipeGenerator = recipeGenerator;
    }

    @Override
    public Recipe recommendRecipe(int targetCalories, Set<String> requiredSkinBenefitTags, Set<String> recentRecipeIds) {
        Recipe candidate = findAntiMonotonyCandidate(requiredSkinBenefitTags, recentRecipeIds)
                .orElseGet(() -> {
                    Recipe generated = recipeGenerator.generateRecipe(targetCalories, requiredSkinBenefitTags);
                    return repository.save(generated);
                });

        return candidate.withMacrosScaledTo(targetCalories);
    }

    /**
     * Anti-monotony rotation: prefer a matching recipe the user has not been
     * served recently. Only falls back to a recently-served match if every
     * matching candidate has already been served (still better than
     * generating an unnecessary duplicate).
     */
    private java.util.Optional<Recipe> findAntiMonotonyCandidate(Set<String> requiredSkinBenefitTags,
                                                                    Set<String> recentRecipeIds) {
        List<Recipe> candidates = repository.findByTargetSkinBenefitsContainingAll(requiredSkinBenefitTags);

        return candidates.stream()
                .filter(recipe -> !recentRecipeIds.contains(recipe.getRecipeId()))
                .findFirst()
                .or(() -> candidates.stream().findFirst());
    }
}
