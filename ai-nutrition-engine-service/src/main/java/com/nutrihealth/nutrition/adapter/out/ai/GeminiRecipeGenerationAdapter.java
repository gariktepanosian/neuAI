package com.nutrihealth.nutrition.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrihealth.nutrition.domain.model.Macros;
import com.nutrihealth.nutrition.domain.model.Recipe;
import com.nutrihealth.nutrition.domain.port.out.RecipeGenerationPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Calls the Gemini API to generate a brand-new recipe when no existing one
 * satisfies the caller's required tags without repeating a recently served
 * dish. The model is instructed to return a single JSON object matching our
 * schema; {@link #parseRecipeResponse} is the boundary that turns that
 * untrusted text back into a domain object, kept separate from the network
 * call so it can be unit tested without a live API key.
 */
@Component
public class GeminiRecipeGenerationAdapter implements RecipeGenerationPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public GeminiRecipeGenerationAdapter(@Value("${nutrihealth.ai.gemini-api-base-url}") String baseUrl,
                                          @Value("${nutrihealth.ai.gemini-api-key}") String apiKey,
                                          @Value("${nutrihealth.ai.gemini-model}") String model,
                                          ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @Override
    public Recipe generateRecipe(int targetCalories, Set<String> requiredSkinBenefitTags) {
        String prompt = buildPrompt(targetCalories, requiredSkinBenefitTags);

        String responseBody = webClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .bodyValue(java.util.Map.of("contents", List.of(
                        java.util.Map.of("parts", List.of(java.util.Map.of("text", prompt))))))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String recipeJson = extractGeneratedText(responseBody);
        return parseRecipeResponse(recipeJson, requiredSkinBenefitTags);
    }

    String buildPrompt(int targetCalories, Set<String> requiredSkinBenefitTags) {
        return """
                Generate a single healthy recipe as a JSON object with exactly these fields:
                title (string), calories (int, near %d), proteinGrams (number), carbsGrams (number),
                fatGrams (number), micronutrientHighlights (string array),
                ingredients (array of {item, grams}). The recipe must support these dietary/skin
                benefit tags: %s. Respond with ONLY the JSON object, no surrounding text.
                """.formatted(targetCalories, String.join(", ", requiredSkinBenefitTags));
    }

    /** Extracts the model's raw text reply from the Gemini API response envelope. */
    String extractGeneratedText(String geminiResponseBody) {
        try {
            JsonNode root = objectMapper.readTree(geminiResponseBody);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Gemini API response envelope", e);
        }
    }

    /** Parses the model-generated recipe JSON into a domain {@link Recipe}. */
    Recipe parseRecipeResponse(String recipeJson, Set<String> requiredSkinBenefitTags) {
        try {
            JsonNode node = objectMapper.readTree(recipeJson);

            Macros macros = new Macros(node.path("calories").asInt(), node.path("proteinGrams").asDouble(),
                    node.path("carbsGrams").asDouble(), node.path("fatGrams").asDouble());

            List<String> micronutrients = new ArrayList<>();
            node.path("micronutrientHighlights").forEach(n -> micronutrients.add(n.asText()));

            List<Recipe.Ingredient> ingredients = new ArrayList<>();
            node.path("ingredients").forEach(n ->
                    ingredients.add(new Recipe.Ingredient(n.path("item").asText(), n.path("grams").asDouble())));

            return new Recipe("rec-" + UUID.randomUUID(), node.path("title").asText(), macros,
                    micronutrients, ingredients, requiredSkinBenefitTags, true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse AI-generated recipe JSON: " + recipeJson, e);
        }
    }
}
