import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/dio_provider.dart';

class DiagnosticReport {
  final String id;
  final String userId;
  final String? clinicId;
  final bool requiresVitaminDAdjustment;
  final List<Observation> observations;
  final DateTime createdAt;

  const DiagnosticReport({
    required this.id,
    required this.userId,
    this.clinicId,
    required this.requiresVitaminDAdjustment,
    required this.observations,
    required this.createdAt,
  });

  factory DiagnosticReport.fromJson(Map<String, dynamic> json) =>
      DiagnosticReport(
        id: json['id'] as String,
        userId: json['userId'] as String,
        clinicId: json['clinicId'] as String?,
        requiresVitaminDAdjustment:
            json['requiresVitaminDAdjustment'] as bool? ?? false,
        observations: (json['observations'] as List<dynamic>? ?? [])
            .map((e) => Observation.fromJson(e as Map<String, dynamic>))
            .toList(),
        createdAt: DateTime.parse(json['createdAt'] as String),
      );
}

class Observation {
  final String loincCode;
  final String displayName;
  final double value;
  final String unit;
  final String interpretation;

  const Observation({
    required this.loincCode,
    required this.displayName,
    required this.value,
    required this.unit,
    required this.interpretation,
  });

  factory Observation.fromJson(Map<String, dynamic> json) => Observation(
        loincCode: json['loincCode'] as String? ?? '',
        displayName: json['displayName'] as String? ?? json['loincCode'] ?? '',
        value: (json['value'] as num?)?.toDouble() ?? 0,
        unit: json['unit'] as String? ?? '',
        interpretation: json['interpretation'] as String? ?? 'NORMAL',
      );
}

class DiagnosticsRepository {
  final Dio _dio;
  static const String _diagnosticsBaseUrl = 'http://10.0.2.2:8083';

  const DiagnosticsRepository(this._dio);

  /// Fetches all diagnostic reports for the authenticated user.
  Future<List<DiagnosticReport>> getReports(String userId) async {
    final response = await Dio(BaseOptions(baseUrl: _diagnosticsBaseUrl))
        .get<List<dynamic>>('/api/v1/fhir/diagnostic-report/$userId');
    return (response.data ?? [])
        .map((e) => DiagnosticReport.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Uploads a FHIR DiagnosticReport JSON file.
  Future<DiagnosticReport> uploadFhirReport({
    required String fhirJson,
    required String userId,
  }) async {
    final response = await Dio(BaseOptions(baseUrl: _diagnosticsBaseUrl))
        .post<Map<String, dynamic>>(
      '/api/v1/fhir/diagnostic-report',
      data: fhirJson,
      options: Options(headers: {
        'Content-Type': 'application/fhir+json',
        'X-User-Id': userId,
      }),
    );
    return DiagnosticReport.fromJson(response.data!);
  }
}

final diagnosticsRepositoryProvider = Provider<DiagnosticsRepository>(
  (ref) => DiagnosticsRepository(ref.watch(dioProvider)),
);

final diagnosticReportsProvider = FutureProvider.autoDispose
    .family<List<DiagnosticReport>, String>((ref, userId) async {
  final repo = ref.watch(diagnosticsRepositoryProvider);
  return repo.getReports(userId);
});
