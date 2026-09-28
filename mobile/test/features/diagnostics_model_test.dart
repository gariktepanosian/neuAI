import 'package:flutter_test/flutter_test.dart';
import '../../../lib/features/diagnostics/data/diagnostics_repository.dart';

void main() {
  group('DiagnosticReport model', () {
    test('fromJson parses all fields correctly', () {
      final json = {
        'id': 'report-1',
        'userId': 'user-1',
        'clinicId': 'clinic-99',
        'requiresVitaminDAdjustment': true,
        'createdAt': '2026-01-15T10:00:00Z',
        'observations': [
          {
            'loincCode': '2888-6',
            'displayName': '25-hydroxyvitamin D3',
            'value': 12.5,
            'unit': 'ng/mL',
            'interpretation': 'LOW',
          }
        ],
      };

      final report = DiagnosticReport.fromJson(json);

      expect(report.id, 'report-1');
      expect(report.userId, 'user-1');
      expect(report.clinicId, 'clinic-99');
      expect(report.requiresVitaminDAdjustment, isTrue);
      expect(report.observations, hasLength(1));
      expect(report.observations.first.loincCode, '2888-6');
      expect(report.observations.first.interpretation, 'LOW');
      expect(report.observations.first.value, 12.5);
    });

    test('fromJson handles missing optional fields', () {
      final json = {
        'id': 'report-2',
        'userId': 'user-2',
        'createdAt': '2026-01-16T08:00:00Z',
        'requiresVitaminDAdjustment': false,
      };

      final report = DiagnosticReport.fromJson(json);
      expect(report.clinicId, isNull);
      expect(report.observations, isEmpty);
    });
  });

  group('Observation model', () {
    test('interpretation defaults to NORMAL when absent', () {
      final json = {
        'loincCode': '789-8',
        'value': 4.5,
        'unit': 'K/uL',
      };
      final obs = Observation.fromJson(json);
      expect(obs.interpretation, 'NORMAL');
    });
  });
}
