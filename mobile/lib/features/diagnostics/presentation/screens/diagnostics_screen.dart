import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import 'package:intl/intl.dart';
import '../../../core/api/token_repository.dart';
import '../../data/diagnostics_repository.dart';

/// Diagnostics portal screen (Phase 3.4).
///
/// - Lists the user's lab reports with biomarker readings
/// - Highlights abnormal values (HIGH/LOW/CRITICAL) with colour coding
/// - Upload button to submit a new FHIR R4 DiagnosticReport JSON
/// - Shows "Vitamin D adjustment required" warning when flagged
class DiagnosticsScreen extends ConsumerWidget {
  const DiagnosticsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tokenRepo = ref.read(tokenRepositoryProvider);

    return FutureBuilder<String?>(
      future: tokenRepo.getUserId(),
      builder: (context, snapshot) {
        final userId = snapshot.data;
        if (userId == null) {
          return Scaffold(
            appBar: AppBar(title: const Text('Diagnostics')),
            body: const Center(child: CircularProgressIndicator()),
          );
        }
        return _DiagnosticsBody(userId: userId);
      },
    );
  }
}

class _DiagnosticsBody extends ConsumerWidget {
  final String userId;
  const _DiagnosticsBody({required this.userId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final reportsAsync = ref.watch(diagnosticReportsProvider(userId));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Diagnostics'),
        actions: [
          IconButton(
            icon: const Icon(Icons.upload_file),
            tooltip: 'Upload FHIR report',
            onPressed: () => _uploadReport(context, ref),
          ),
        ],
      ),
      body: reportsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (reports) => reports.isEmpty
            ? Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.biotech_outlined,
                        size: 64, color: Colors.grey),
                    const SizedBox(height: 16),
                    const Text('No diagnostic reports yet'),
                    const SizedBox(height: 8),
                    ElevatedButton.icon(
                      icon: const Icon(Icons.upload_file),
                      label: const Text('Upload Report'),
                      onPressed: () => _uploadReport(context, ref),
                    ),
                  ],
                ),
              )
            : ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: reports.length,
                separatorBuilder: (_, __) => const SizedBox(height: 12),
                itemBuilder: (_, i) => _ReportCard(report: reports[i]),
              ),
      ),
    );
  }

  Future<void> _uploadReport(BuildContext context, WidgetRef ref) async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['json'],
    );
    if (result == null || result.files.isEmpty) return;

    final file = result.files.first;
    if (file.bytes == null) return;

    final fhirJson = String.fromCharCodes(file.bytes!);
    final repo = ref.read(diagnosticsRepositoryProvider);

    try {
      await repo.uploadFhirReport(fhirJson: fhirJson, userId: userId);
      ref.refresh(diagnosticReportsProvider(userId));
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Report uploaded successfully')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Upload failed: $e'),
              backgroundColor: Colors.red),
        );
      }
    }
  }
}

class _ReportCard extends StatelessWidget {
  final DiagnosticReport report;
  const _ReportCard({required this.report});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Icon(Icons.science_outlined,
                  color: Theme.of(context).colorScheme.primary),
              const SizedBox(width: 8),
              Text(
                DateFormat('MMM d, yyyy').format(report.createdAt),
                style: Theme.of(context)
                    .textTheme
                    .titleSmall
                    ?.copyWith(fontWeight: FontWeight.bold),
              ),
              if (report.clinicId != null) ...[
                const Spacer(),
                Chip(
                  label: Text(report.clinicId!,
                      style: const TextStyle(fontSize: 11)),
                  padding: EdgeInsets.zero,
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
              ],
            ]),

            // Vitamin D warning
            if (report.requiresVitaminDAdjustment) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.orange.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.orange),
                ),
                child: Row(children: [
                  const Icon(Icons.warning_amber_rounded,
                      color: Colors.orange, size: 16),
                  const SizedBox(width: 6),
                  const Expanded(
                    child: Text('Vitamin D dietary adjustment recommended',
                        style: TextStyle(fontSize: 12, color: Colors.orange)),
                  ),
                ]),
              ),
            ],

            if (report.observations.isNotEmpty) ...[
              const SizedBox(height: 12),
              ...report.observations.map((o) => _ObservationRow(obs: o)),
            ],
          ],
        ),
      ),
    );
  }
}

class _ObservationRow extends StatelessWidget {
  final Observation obs;
  const _ObservationRow({required this.obs});

  Color _interpretationColor() {
    return switch (obs.interpretation.toUpperCase()) {
      'CRITICAL' => Colors.red,
      'HIGH' => Colors.orange,
      'LOW' => Colors.blue,
      _ => Colors.green,
    };
  }

  @override
  Widget build(BuildContext context) {
    final color = _interpretationColor();
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(children: [
        Expanded(
            child: Text(obs.displayName,
                style: const TextStyle(fontSize: 13))),
        Text('${obs.value.toStringAsFixed(1)} ${obs.unit}',
            style: const TextStyle(fontSize: 13)),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
          decoration: BoxDecoration(
            color: color.withOpacity(0.15),
            borderRadius: BorderRadius.circular(4),
          ),
          child: Text(obs.interpretation,
              style: TextStyle(color: color, fontSize: 11,
                  fontWeight: FontWeight.bold)),
        ),
      ]),
    );
  }
}
