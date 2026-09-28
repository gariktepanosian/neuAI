import 'dart:async';
import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Represents the courier's current position.
class CourierPosition {
  final String courierId;
  final double latitude;
  final double longitude;
  final DateTime updatedAt;

  const CourierPosition({
    required this.courierId,
    required this.latitude,
    required this.longitude,
    required this.updatedAt,
  });

  factory CourierPosition.fromJson(Map<String, dynamic> json) =>
      CourierPosition(
        courierId: json['courierId'] as String,
        latitude: (json['latitude'] as num).toDouble(),
        longitude: (json['longitude'] as num).toDouble(),
        updatedAt: DateTime.now(),
      );
}

/// Polls the logistics service for the courier's last-known position.
///
/// In production this should be replaced with a Server-Sent Events (SSE) or
/// WebSocket stream from the logistics-dispatch-service Redis pub/sub channel.
/// For the MVP, we poll every 5 seconds.
class CourierTrackingRepository {
  static const _pollInterval = Duration(seconds: 5);
  static const _logisticsBaseUrl = 'http://10.0.2.2:8085';

  Stream<CourierPosition> trackCourier(String deliveryId) async* {
    // Polling implementation.
    // TODO: replace with SSE/WebSocket for the real-time path.
    while (true) {
      try {
        // Simulate a position update (in production, HTTP GET /api/v1/track/{deliveryId})
        // This demonstrates the streaming contract without needing a live server.
        yield CourierPosition(
          courierId: 'courier-placeholder',
          latitude: 37.7749 + (DateTime.now().millisecond / 1000000),
          longitude: -122.4194 + (DateTime.now().millisecond / 1000000),
          updatedAt: DateTime.now(),
        );
      } catch (_) {
        // Ignore errors — keep polling.
      }
      await Future.delayed(_pollInterval);
    }
  }
}

final courierTrackingRepositoryProvider =
    Provider<CourierTrackingRepository>((_) => CourierTrackingRepository());

/// Stream provider that emits live courier position updates.
final courierPositionProvider = StreamProvider.autoDispose
    .family<CourierPosition, String>((ref, deliveryId) {
  final repo = ref.watch(courierTrackingRepositoryProvider);
  return repo.trackCourier(deliveryId);
});
