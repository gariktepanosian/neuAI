import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/dio_provider.dart';

/// Domain model for a recipe returned by the ai-nutrition-engine-service.
class Recipe {
  final String id;
  final String title;
  final int calories;
  final double proteinGrams;
  final double carbsGrams;
  final double fatGrams;
  final List<String> micronutrientHighlights;
  final List<String> skinBenefitTags;

  const Recipe({
    required this.id,
    required this.title,
    required this.calories,
    required this.proteinGrams,
    required this.carbsGrams,
    required this.fatGrams,
    required this.micronutrientHighlights,
    required this.skinBenefitTags,
  });

  factory Recipe.fromJson(Map<String, dynamic> json) => Recipe(
        id: json['id'] as String? ?? '',
        title: json['title'] as String,
        calories: json['calories'] as int,
        proteinGrams: (json['proteinGrams'] as num).toDouble(),
        carbsGrams: (json['carbsGrams'] as num).toDouble(),
        fatGrams: (json['fatGrams'] as num).toDouble(),
        micronutrientHighlights:
            List<String>.from(json['micronutrientHighlights'] ?? []),
        skinBenefitTags: List<String>.from(json['skinBenefitTags'] ?? []),
      );
}

class MenuRepository {
  final Dio _dio;
  static const String _nutritionBaseUrl = 'http://10.0.2.2:8084';

  const MenuRepository(this._dio);

  Future<List<Recipe>> getDailyMenu({
    required int targetCalories,
    List<String> requiredTags = const [],
    List<String> recentRecipeIds = const [],
  }) async {
    final response = await Dio(BaseOptions(baseUrl: _nutritionBaseUrl))
        .get<List<dynamic>>(
      '/api/v1/menu',
      queryParameters: {
        'targetCalories': targetCalories,
        if (requiredTags.isNotEmpty) 'requiredTags': requiredTags,
        if (recentRecipeIds.isNotEmpty) 'excludeIds': recentRecipeIds,
      },
    );
    return (response.data ?? [])
        .map((e) => Recipe.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}

final menuRepositoryProvider = Provider<MenuRepository>(
  (ref) => MenuRepository(ref.watch(dioProvider)),
);

/// Riverpod async provider for the daily menu.
final dailyMenuProvider = FutureProvider.autoDispose
    .family<List<Recipe>, int>((ref, targetCalories) async {
  final repo = ref.watch(menuRepositoryProvider);
  return repo.getDailyMenu(targetCalories: targetCalories);
});
