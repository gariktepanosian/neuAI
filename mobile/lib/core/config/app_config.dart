/// Application-wide configuration.
///
/// Values are read from `--dart-define` flags set during build, falling back
/// to local-dev defaults. In CI/CD, supply the real values via GitHub Actions
/// secrets mapped to `--dart-define` in the Flutter build step.
class AppConfig {
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8082', // Android emulator → localhost
  );

  static const String geminiApiKey = String.fromEnvironment(
    'GEMINI_API_KEY',
    defaultValue: '',
  );

  static const String googleMapsApiKey = String.fromEnvironment(
    'GOOGLE_MAPS_API_KEY',
    defaultValue: '',
  );
}
