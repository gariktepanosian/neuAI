import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/register_screen.dart';
import '../../features/menu/presentation/screens/menu_screen.dart';
import '../../features/diagnostics/presentation/screens/diagnostics_screen.dart';
import '../../features/tracking/presentation/screens/tracking_screen.dart';
import '../api/token_repository.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final tokenRepo = ref.watch(tokenRepositoryProvider);

  return GoRouter(
    initialLocation: '/login',
    redirect: (context, state) async {
      final isAuthenticated = await tokenRepo.isTokenValid();
      final onAuthPage = state.matchedLocation == '/login' ||
          state.matchedLocation == '/register';

      if (!isAuthenticated && !onAuthPage) return '/login';
      if (isAuthenticated && onAuthPage) return '/menu';
      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(path: '/register', builder: (_, __) => const RegisterScreen()),
      GoRoute(path: '/menu', builder: (_, __) => const MenuScreen()),
      GoRoute(path: '/diagnostics', builder: (_, __) => const DiagnosticsScreen()),
      GoRoute(
        path: '/tracking/:deliveryId',
        builder: (_, state) =>
            TrackingScreen(deliveryId: state.pathParameters['deliveryId']!),
      ),
    ],
  );
});
