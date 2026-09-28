# ai-nutrition-engine-service

AI-driven recipe generation and menu personalization microservice for
NutriHealth AI.

## Structure (Hexagonal / Ports & Adapters)

```
domain/model         - Macros (calorie-preserving-ratio scaling), Recipe (tag matching)
domain/port/in         - GenerateDailyMenuUseCase (inbound port)
domain/port/out        - RecipeRepositoryPort, RecipeGenerationPort
application/service    - MenuRecommendationService (anti-monotony selection + macro scaling)
adapter/in/web          - MenuController (POST /api/v1/menu/daily)
adapter/out/persistence - Mongo adapter for the `AI_personalized_recipes` collection
adapter/out/ai          - GeminiRecipeGenerationAdapter (calls the Gemini API when no candidate fits)
```

## Recommendation flow

1. Look up existing recipes matching all of the caller's `requiredSkinBenefitTags`
   (e.g. derived from a LOW vitamin D FHIR observation upstream).
2. Anti-monotony rotation: prefer a match not in `recentRecipeIds`; only reuse
   a recently-served recipe if every matching candidate has already been served.
3. If nothing matches at all, call Gemini to generate a new recipe and persist it.
4. Scale the chosen recipe's macros to the caller's `targetCalories`, preserving
   its original macro ratio (`Macros.scaleToCalories`).

## Running locally

Requires MongoDB (defaults to `mongodb://localhost:27017/nutrition_db`, override
via `MONGODB_URI`) and, for real AI generation, a Gemini API key (`GEMINI_API_KEY`).

```bash
./mvnw spring-boot:run
```

## Notes / deviations from the spec

- Built against **Java 17**, matching the other services in this repo.
- The spec names "Gemini 3.1 Pro"; no such model id exists publicly as of this
  writing, so `nutrihealth.ai.gemini-model` defaults to `gemini-1.5-pro` and
  is fully configurable — update it once the intended model is confirmed.
- `GeminiRecipeGenerationAdapter`'s network call (`generateRecipe`) is not unit
  tested — no live API key/network in this environment. Its prompt-building
  and response-parsing logic *are* unit tested in isolation
  (`GeminiRecipeGenerationAdapterTest`), since that's where untrusted
  model output is parsed back into a domain object.
- No Spring context boot test here (unlike the JPA-backed services): Mongo has
  no equivalent to H2's in-memory mode without adding an embedded-Mongo
  dependency, so this service currently relies on the unit test suite only.
