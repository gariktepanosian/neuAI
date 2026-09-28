import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../config/app_config.dart';
import 'token_repository.dart';

/// Creates and configures the [Dio] HTTP client with:
/// - Base URL from [AppConfig]
/// - JWT auth interceptor (reads token from [TokenRepository])
/// - Retry-on-401 with token refresh (placeholder)
/// - Logging interceptor in debug mode
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: AppConfig.baseUrl,
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(seconds: 30),
    headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
  ));

  final tokenRepo = ref.read(tokenRepositoryProvider);

  // JWT Authorization interceptor
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final token = await tokenRepo.getAccessToken();
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      handler.next(options);
    },
    onError: (error, handler) async {
      if (error.response?.statusCode == 401) {
        // Token expired — clear it and let the router redirect to login.
        await tokenRepo.clearToken();
      }
      handler.next(error);
    },
  ));

  return dio;
});
