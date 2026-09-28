package com.nutrihealth.nutrition.adapter.in.web;

import com.nutrihealth.nutrition.domain.model.Recipe;
import com.nutrihealth.nutrition.domain.port.in.GenerateDailyMenuUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    private final GenerateDailyMenuUseCase useCase;

    public MenuController(GenerateDailyMenuUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/daily")
    public RecipeResponse recommendDailyRecipe(@Valid @RequestBody DailyMenuRequest request) {
        Recipe recipe = useCase.recommendRecipe(request.targetCalories(),
                request.requiredSkinBenefitTags(), request.recentRecipeIds());
        return RecipeResponse.from(recipe);
    }

    public record DailyMenuRequest(
            @Positive int targetCalories,
            Set<String> requiredSkinBenefitTags,
            Set<String> recentRecipeIds) {

        public DailyMenuRequest {
            requiredSkinBenefitTags = requiredSkinBenefitTags == null ? Set.of() : requiredSkinBenefitTags;
            recentRecipeIds = recentRecipeIds == null ? Set.of() : recentRecipeIds;
        }
    }
}
