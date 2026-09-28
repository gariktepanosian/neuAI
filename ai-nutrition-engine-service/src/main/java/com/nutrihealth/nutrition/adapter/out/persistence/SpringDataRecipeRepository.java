package com.nutrihealth.nutrition.adapter.out.persistence;

import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SpringDataRecipeRepository extends MongoRepository<RecipeDocument, String> {

    @Query("{ 'targetSkinBenefits': { $all: ?0 } }")
    List<RecipeDocument> findByTargetSkinBenefitsContainingAll(Set<String> requiredSkinBenefitTags);
}
