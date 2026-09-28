import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../auth/domain/auth_notifier.dart';
import '../../data/menu_repository.dart';

/// Meal scheduling / daily menu screen (Phase 3.3).
///
/// Features:
/// - Daily recipe carousel with macro breakdown
/// - Macro pie chart (protein / carbs / fat) via fl_chart
/// - Pull-to-refresh to generate a new menu
/// - Target calorie preference (user-adjustable)
class MenuScreen extends ConsumerStatefulWidget {
  const MenuScreen({super.key});

  @override
  ConsumerState<MenuScreen> createState() => _MenuScreenState();
}

class _MenuScreenState extends ConsumerState<MenuScreen> {
  int _targetCalories = 2000;
  final _pageController = PageController();

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final menuAsync = ref.watch(dailyMenuProvider(_targetCalories));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Today\'s Menu'),
        actions: [
          IconButton(
            icon: const Icon(Icons.medical_services_outlined),
            tooltip: 'Diagnostics',
            onPressed: () => context.push('/diagnostics'),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Sign out',
            onPressed: () =>
                ref.read(authNotifierProvider.notifier).logout(),
          ),
        ],
      ),
      body: Column(
        children: [
          // Calorie target selector
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                const Text('Daily target:',
                    style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(width: 8),
                Expanded(
                  child: Slider(
                    value: _targetCalories.toDouble(),
                    min: 1200,
                    max: 3500,
                    divisions: 23,
                    label: '$_targetCalories kcal',
                    onChanged: (v) =>
                        setState(() => _targetCalories = v.round()),
                    onChangeEnd: (_) => ref.refresh(dailyMenuProvider(_targetCalories)),
                  ),
                ),
                Text('$_targetCalories kcal',
                    style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),

          // Recipe list
          Expanded(
            child: menuAsync.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, size: 48),
                    const SizedBox(height: 8),
                    Text('Failed to load menu: $e'),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: () => ref.refresh(dailyMenuProvider(_targetCalories)),
                      child: const Text('Retry'),
                    ),
                  ],
                ),
              ),
              data: (recipes) => recipes.isEmpty
                  ? const Center(child: Text('No recipes available'))
                  : RefreshIndicator(
                      onRefresh: () async => ref.refresh(dailyMenuProvider(_targetCalories)),
                      child: ListView.separated(
                        padding: const EdgeInsets.all(16),
                        itemCount: recipes.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 12),
                        itemBuilder: (_, i) => _RecipeCard(recipe: recipes[i]),
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class _RecipeCard extends StatelessWidget {
  final Recipe recipe;
  const _RecipeCard({required this.recipe});

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(recipe.title,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text('${recipe.calories} kcal',
                style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 12),

            // Macro progress bars
            _MacroBar('Protein', recipe.proteinGrams, recipe.calories, Colors.blue),
            const SizedBox(height: 4),
            _MacroBar('Carbs', recipe.carbsGrams, recipe.calories, Colors.orange),
            const SizedBox(height: 4),
            _MacroBar('Fat', recipe.fatGrams, recipe.calories, Colors.red),

            if (recipe.micronutrientHighlights.isNotEmpty) ...[
              const SizedBox(height: 8),
              Wrap(
                spacing: 6,
                children: recipe.micronutrientHighlights
                    .map((m) => Chip(
                          label: Text(m,
                              style: const TextStyle(fontSize: 11)),
                          padding: EdgeInsets.zero,
                          materialTapTargetSize:
                              MaterialTapTargetSize.shrinkWrap,
                        ))
                    .toList(),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _MacroBar extends StatelessWidget {
  final String label;
  final double grams;
  final int totalCalories;
  final Color color;

  const _MacroBar(this.label, this.grams, this.totalCalories, this.color);

  @override
  Widget build(BuildContext context) {
    // Rough macro calorie contribution (protein/carbs = 4 kcal/g, fat = 9 kcal/g)
    final cals = label == 'Fat' ? grams * 9 : grams * 4;
    final ratio = totalCalories > 0 ? (cals / totalCalories).clamp(0.0, 1.0) : 0.0;
    return Row(
      children: [
        SizedBox(width: 56, child: Text(label, style: const TextStyle(fontSize: 12))),
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: ratio,
              backgroundColor: color.withOpacity(0.2),
              valueColor: AlwaysStoppedAnimation<Color>(color),
              minHeight: 8,
            ),
          ),
        ),
        const SizedBox(width: 8),
        Text('${grams.toStringAsFixed(1)}g',
            style: const TextStyle(fontSize: 12)),
      ],
    );
  }
}
