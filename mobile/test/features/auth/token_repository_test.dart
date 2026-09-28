import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../lib/core/api/token_repository.dart';

class _MockSecureStorage extends Mock implements FlutterSecureStorage {}

void main() {
  late _MockSecureStorage mockStorage;
  late TokenRepository repository;

  setUp(() {
    mockStorage = _MockSecureStorage();
    repository = TokenRepository(mockStorage);
  });

  group('TokenRepository', () {
    test('saveToken writes all three keys', () async {
      when(() => mockStorage.write(
          key: any(named: 'key'), value: any(named: 'value')))
          .thenAnswer((_) async {});

      await repository.saveToken(
        accessToken: 'test.jwt.token',
        expiresAtEpochSeconds: 9999999999,
        userId: 'user-123',
      );

      verify(() => mockStorage.write(key: 'access_token', value: 'test.jwt.token')).called(1);
      verify(() => mockStorage.write(key: 'access_token_expires_at', value: '9999999999')).called(1);
      verify(() => mockStorage.write(key: 'user_id', value: 'user-123')).called(1);
    });

    test('isTokenValid returns true when expiry is in the future', () async {
      final futureExp = (DateTime.now().millisecondsSinceEpoch ~/ 1000) + 3600;
      when(() => mockStorage.read(key: 'access_token_expires_at'))
          .thenAnswer((_) async => futureExp.toString());

      final valid = await repository.isTokenValid();
      expect(valid, isTrue);
    });

    test('isTokenValid returns false when expiry is in the past', () async {
      const pastExp = 1000000;
      when(() => mockStorage.read(key: 'access_token_expires_at'))
          .thenAnswer((_) async => pastExp.toString());

      final valid = await repository.isTokenValid();
      expect(valid, isFalse);
    });

    test('isTokenValid returns false when no token stored', () async {
      when(() => mockStorage.read(key: 'access_token_expires_at'))
          .thenAnswer((_) async => null);

      final valid = await repository.isTokenValid();
      expect(valid, isFalse);
    });

    test('clearToken deletes all three keys', () async {
      when(() => mockStorage.delete(key: any(named: 'key')))
          .thenAnswer((_) async {});

      await repository.clearToken();

      verify(() => mockStorage.delete(key: 'access_token')).called(1);
      verify(() => mockStorage.delete(key: 'access_token_expires_at')).called(1);
      verify(() => mockStorage.delete(key: 'user_id')).called(1);
    });
  });
}
