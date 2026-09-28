package com.nutrihealth.nutrition.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrihealth.nutrition.domain.model.Recipe;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GeminiRecipeGenerationAdapterTest {

    private final GeminiRecipeGenerationAdapter adapter = new GeminiRecipeGenerationAdapter(
            "https://example.invalid", "test-key", "gemini-1.5-pro", new ObjectMapper());

    @Test
    void buildPromptIncludesCalorieTargetAndTags() {
        String prompt = adapter.buildPrompt(1200, Set.of("OILY_SKIN_BALANCING"));

        assertThat(prompt).contains("1200").contains("OILY_SKIN_BALANCING").contains("JSON object");
    }

    @Test
    void extractGeneratedTextReadsGeminiResponseEnvelope() {
        String geminiEnvelope = """
                {"candidates":[{"content":{"parts":[{"text":"{\\"title\\":\\"Test\\"}"}]}}]}
                """;

        String text = adapter.extractGeneratedText(geminiEnvelope);

        assertThat(text).isEqualTo("{\"title\":\"Test\"}");
    }

    @Test
    void parseRecipeResponseBuildsDomainRecipe() {
        String recipeJson = """
                {
                  "title": "High-Protein Salmon Bowl",
                  "calories": 650,
                  "proteinGrams": 48.0,
                  "carbsGrams": 52.0,
                  "fatGrams": 22.0,
                  "micronutrientHighlights": ["Vitamin D", "Omega-3"],
                  "ingredients": [{"item": "Salmon", "grams": 200}]
                }
                """;

        Recipe recipe = adapter.parseRecipeResponse(recipeJson, Set.of("OILY_SKIN_BALANCING"));

        assertThat(recipe.getTitle()).isEqualTo("High-Protein Salmon Bowl");
        assertThat(recipe.getMacros().calories()).isEqualTo(650);
        assertThat(recipe.getMicronutrientHighlights()).containsExactly("Vitamin D", "Omega-3");
        assertThat(recipe.getIngredients()).containsExactly(new Recipe.Ingredient("Salmon", 200));
        assertThat(recipe.getTargetSkinBenefits()).isEqualTo(Set.of("OILY_SKIN_BALANCING"));
        assertThat(recipe.isAntiMonotonyAlternative()).isTrue();
    }
}
