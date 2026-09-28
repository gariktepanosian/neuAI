import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:local_auth/local_auth.dart';
import '../data/auth_repository.dart';
import '../../../core/api/token_repository.dart';

enum AuthStatus { initial, loading, authenticated, unauthenticated, error }

class AuthState {
  final AuthStatus status;
  final String? errorMessage;
  const AuthState({this.status = AuthStatus.initial, this.errorMessage});
  AuthState copyWith({AuthStatus? status, String? errorMessage}) =>
      AuthState(status: status ?? this.status, errorMessage: errorMessage);
}

class AuthNotifier extends StateNotifier<AuthState> {
  final AuthRepository _repo;
  final TokenRepository _tokenRepo;
  final LocalAuthentication _localAuth;

  AuthNotifier(this._repo, this._tokenRepo, this._localAuth)
      : super(const AuthState()) {
    _checkInitialAuth();
  }

  Future<void> _checkInitialAuth() async {
    final valid = await _tokenRepo.isTokenValid();
    state = state.copyWith(
        status: valid ? AuthStatus.authenticated : AuthStatus.unauthenticated);
  }

  Future<void> login(String email, String password) async {
    state = state.copyWith(status: AuthStatus.loading);
    try {
      await _repo.login(email: email, password: password);
      state = state.copyWith(status: AuthStatus.authenticated);
    } catch (e) {
      state = state.copyWith(
          status: AuthStatus.error,
          errorMessage: _extractError(e));
    }
  }

  Future<void> register(String email, String password) async {
    state = state.copyWith(status: AuthStatus.loading);
    try {
      await _repo.register(email: email, password: password);
      // After registration, log in immediately.
      await _repo.login(email: email, password: password);
      state = state.copyWith(status: AuthStatus.authenticated);
    } catch (e) {
      state = state.copyWith(
          status: AuthStatus.error,
          errorMessage: _extractError(e));
    }
  }

  /// Performs biometric authentication using the device's fingerprint/Face ID,
  /// then calls the backend /login/biometric endpoint with the resulting token.
  Future<void> biometricLogin() async {
    state = state.copyWith(status: AuthStatus.loading);
    try {
      // 1. Check if biometrics are available on this device.
      final canCheck = await _localAuth.canCheckBiometrics;
      if (!canCheck) {
        state = state.copyWith(
            status: AuthStatus.error,
            errorMessage: 'Biometric authentication is not available on this device');
        return;
      }

      // 2. Prompt the user for biometric.
      final authenticated = await _localAuth.authenticate(
        localizedReason: 'Authenticate to access NutriHealth AI',
        options: const AuthenticationOptions(
          biometricOnly: true,
          stickyAuth: true,
        ),
      );

      if (!authenticated) {
        state = state.copyWith(
            status: AuthStatus.unauthenticated,
            errorMessage: 'Biometric authentication cancelled');
        return;
      }

      // 3. Use the stored userId (from a previous login) to call the backend.
      final userId = await _tokenRepo.getUserId();
      if (userId == null) {
        state = state.copyWith(
            status: AuthStatus.unauthenticated,
            errorMessage: 'Please log in with your password first to enable biometrics');
        return;
      }

      // 4. Create a biometric token (simplified: use userId as token in MVP).
      //    In production, sign a challenge with the Android Keystore private key.
      await _repo.biometricLogin(userId: userId, biometricToken: userId);
      state = state.copyWith(status: AuthStatus.authenticated);
    } catch (e) {
      state = state.copyWith(
          status: AuthStatus.error,
          errorMessage: _extractError(e));
    }
  }

  Future<void> logout() async {
    await _repo.logout();
    state = state.copyWith(status: AuthStatus.unauthenticated);
  }

  String _extractError(Object e) {
    if (e is Exception) return e.toString().replaceFirst('Exception: ', '');
    return 'An unexpected error occurred';
  }
}

final authNotifierProvider =
    StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(
    ref.watch(authRepositoryProvider),
    ref.watch(tokenRepositoryProvider),
    LocalAuthentication(),
  );
});
