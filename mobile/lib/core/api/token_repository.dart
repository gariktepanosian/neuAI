import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Provides the [FlutterSecureStorage] singleton.
final secureStorageProvider = Provider<FlutterSecureStorage>(
  (_) => const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
    iOptions: IOSOptions(accessibility: KeychainAccessibility.first_unlock),
  ),
);

/// Token repository — stores and retrieves the JWT access token from the
/// device's hardware-backed Keychain (iOS) or EncryptedSharedPreferences (Android).
class TokenRepository {
  static const _accessTokenKey = 'access_token';
  static const _expiresAtKey = 'access_token_expires_at';
  static const _userIdKey = 'user_id';

  final FlutterSecureStorage _storage;

  const TokenRepository(this._storage);

  Future<void> saveToken({
    required String accessToken,
    required int expiresAtEpochSeconds,
    required String userId,
  }) async {
    await Future.wait([
      _storage.write(key: _accessTokenKey, value: accessToken),
      _storage.write(key: _expiresAtKey, value: expiresAtEpochSeconds.toString()),
      _storage.write(key: _userIdKey, value: userId),
    ]);
  }

  Future<String?> getAccessToken() => _storage.read(key: _accessTokenKey);
  Future<String?> getUserId() => _storage.read(key: _userIdKey);

  Future<bool> isTokenValid() async {
    final expiresAt = await _storage.read(key: _expiresAtKey);
    if (expiresAt == null) return false;
    final exp = int.tryParse(expiresAt) ?? 0;
    // Consider expired 60 s before actual expiry to avoid race conditions.
    return DateTime.now().millisecondsSinceEpoch < (exp - 60) * 1000;
  }

  Future<void> clearToken() async {
    await Future.wait([
      _storage.delete(key: _accessTokenKey),
      _storage.delete(key: _expiresAtKey),
      _storage.delete(key: _userIdKey),
    ]);
  }
}

final tokenRepositoryProvider = Provider<TokenRepository>(
  (ref) => TokenRepository(ref.watch(secureStorageProvider)),
);
