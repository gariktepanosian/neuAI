import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/dio_provider.dart';
import '../../../core/api/token_repository.dart';

class AuthRepository {
  final Dio _dio;
  final TokenRepository _tokenRepo;

  const AuthRepository(this._dio, this._tokenRepo);

  Future<void> register({
    required String email,
    required String password,
  }) async {
    await _dio.post('/api/v1/auth/register', data: {
      'email': email,
      'password': password,
    });
  }

  Future<void> login({
    required String email,
    required String password,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/auth/login',
      data: {'email': email, 'password': password},
    );
    final data = response.data!;
    // Decode userId from JWT sub claim
    final parts = (data['accessToken'] as String).split('.');
    final payload = String.fromCharCodes(
        _base64Decode(parts[1]));
    final sub = RegExp(r'"sub"\s*:\s*"([^"]+)"').firstMatch(payload)?.group(1) ?? '';

    await _tokenRepo.saveToken(
      accessToken: data['accessToken'] as String,
      expiresAtEpochSeconds: data['expiresAtEpochSeconds'] as int,
      userId: sub,
    );
  }

  Future<void> biometricLogin({
    required String userId,
    required String biometricToken,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/auth/login/biometric',
      data: {'userId': userId, 'biometricToken': biometricToken},
    );
    final data = response.data!;
    await _tokenRepo.saveToken(
      accessToken: data['accessToken'] as String,
      expiresAtEpochSeconds: data['expiresAtEpochSeconds'] as int,
      userId: userId,
    );
  }

  Future<void> logout() => _tokenRepo.clearToken();

  // Simple base64-url to bytes helper (no external dependency)
  static List<int> _base64Decode(String input) {
    final normalized = input
        .replaceAll('-', '+')
        .replaceAll('_', '/');
    final padded = normalized.padRight(
        normalized.length + (4 - normalized.length % 4) % 4, '=');
    return Uri.parse('data:application/octet-stream;base64,$padded')
        .data!
        .contentAsBytes();
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    ref.watch(dioProvider),
    ref.watch(tokenRepositoryProvider),
  );
});
