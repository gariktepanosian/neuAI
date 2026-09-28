package com.nutrihealth.nutrition.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutrihealth.nutrition.domain.model.Macros;
import com.nutrihealth.nutrition.domain.model.Recipe;
import com.nutrihealth.nutrition.domain.port.out.RecipeGenerationPort;
import com.nutrihealth.nutrition.domain.port.out.RecipeRepositoryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MenuRecommendationServiceTest {

    private Recipe recipe(String id) {
        return new Recipe(id, "Recipe " + id, new Macros(600, 40, 50, 20),
                List.of("Vitamin D"), List.of(), Set.of("OILY_SKIN_BALANCING"), false);
    }

    private RecipeRepositoryPort repositoryReturning(List<Recipe> candidates) {
        List<Recipe> saved = new ArrayList<>();
        return new RecipeRepositoryPort() {
            @Override
            public Recipe save(Recipe recipe) {
                saved.add(recipe);
                return recipe;
            }

            @Override
            public List<Recipe> findByTargetSkinBenefitsContainingAll(Set<String> requiredSkinBenefitTags) {
                return candidates;
            }
        };
    }

    @Test
    void prefersCandidateNotRecentlyServed() {
        Recipe recentlyServed = recipe("rec-1");
        Recipe freshCandidate = recipe("rec-2");
        RecipeRepositoryPort repository = repositoryReturning(List.of(recentlyServed, freshCandidate));
        RecipeGenerationPort generator = (cal, tags) -> {
            throw new AssertionError("should not generate when a fresh candidate exists");
        };
        MenuRecommendationService service = new MenuRecommendationService(repository, generator);

        Recipe result = service.recommendRecipe(1200, Set.of("OILY_SKIN_BALANCING"), Set.of("rec-1"));

        assertThat(result.getRecipeId()).isEqualTo("rec-2");
        assertThat(result.getMacros().calories()).isEqualTo(1200);
    }

    @Test
    void fallsBackToRecentlyServedWhenNoFreshCandidateExists() {
        Recipe onlyCandidate = recipe("rec-1");
        RecipeRepositoryPort repository = repositoryReturning(List.of(onlyCandidate));
        RecipeGenerationPort generator = (cal, tags) -> {
            throw new AssertionError("should not generate when a matching candidate exists at all");
        };
        MenuRecommendationService service = new MenuRecommendationService(repository, generator);

        Recipe result = service.recommendRecipe(1200, Set.of("OILY_SKIN_BALANCING"), Set.of("rec-1"));

        assertThat(result.getRecipeId()).isEqualTo("rec-1");
    }

    @Test
    void generatesAndPersistsNewRecipeWhenNoCandidateMatches() {
        RecipeRepositoryPort repository = repositoryReturning(List.of());
        Recipe generated = recipe("rec-generated");
        RecipeGenerationPort generator = (cal, tags) -> generated;
        MenuRecommendationService service = new MenuRecommendationService(repository, generator);

        Recipe result = service.recommendRecipe(1200, Set.of("OILY_SKIN_BALANCING"), Set.of());

        assertThat(result.getRecipeId()).isEqualTo("rec-generated");
    }
}
