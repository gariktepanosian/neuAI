import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:intl/intl.dart';
import '../../data/courier_tracking_repository.dart';

/// Live courier tracking map screen (Phase 3.5).
///
/// Displays:
/// - Google Map with the courier's current position (animated marker)
/// - Delivery ID and last update timestamp
/// - Status card at the bottom
class TrackingScreen extends ConsumerStatefulWidget {
  final String deliveryId;
  const TrackingScreen({super.key, required this.deliveryId});

  @override
  ConsumerState<TrackingScreen> createState() => _TrackingScreenState();
}

class _TrackingScreenState extends ConsumerState<TrackingScreen> {
  GoogleMapController? _mapController;
  static const _defaultZoom = 15.0;
  static const _san_francisco = LatLng(37.7749, -122.4194);

  @override
  void dispose() {
    _mapController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final positionAsync =
        ref.watch(courierPositionProvider(widget.deliveryId));

    return Scaffold(
      appBar: AppBar(
        title: Text('Delivery #${widget.deliveryId.substring(0, 8)}...'),
      ),
      body: Stack(
        children: [
          // Google Map
          positionAsync.when(
            loading: () => GoogleMap(
              initialCameraPosition: const CameraPosition(
                  target: _san_francisco, zoom: _defaultZoom),
              onMapCreated: (c) => _mapController = c,
            ),
            error: (e, _) => Center(child: Text('Tracking unavailable: $e')),
            data: (position) {
              final latlng = LatLng(position.latitude, position.longitude);
              // Animate camera to courier position.
              _mapController?.animateCamera(
                  CameraUpdate.newLatLng(latlng));
              return GoogleMap(
                initialCameraPosition: CameraPosition(
                    target: latlng, zoom: _defaultZoom),
                onMapCreated: (c) => _mapController = c,
                markers: {
                  Marker(
                    markerId: const MarkerId('courier'),
                    position: latlng,
                    infoWindow: InfoWindow(
                      title: 'Your courier',
                      snippet: 'Updated ${DateFormat.Hm().format(position.updatedAt)}',
                    ),
                    icon: BitmapDescriptor.defaultMarkerWithHue(
                        BitmapDescriptor.hueGreen),
                  ),
                },
              );
            },
          ),

          // Status panel at bottom
          Positioned(
            bottom: 0,
            left: 0,
            right: 0,
            child: Card(
              margin: const EdgeInsets.all(16),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: positionAsync.when(
                  loading: () => const Row(children: [
                    CircularProgressIndicator(strokeWidth: 2),
                    SizedBox(width: 12),
                    Text('Locating your courier...'),
                  ]),
                  error: (_, __) => const Row(children: [
                    Icon(Icons.location_off, color: Colors.grey),
                    SizedBox(width: 8),
                    Text('Tracking unavailable'),
                  ]),
                  data: (pos) => Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Row(children: [
                        Container(
                          width: 10, height: 10,
                          decoration: const BoxDecoration(
                              color: Colors.green,
                              shape: BoxShape.circle),
                        ),
                        const SizedBox(width: 8),
                        const Text('Courier on the way',
                            style: TextStyle(fontWeight: FontWeight.bold)),
                        const Spacer(),
                        Text(
                          'Updated ${DateFormat.Hm().format(pos.updatedAt)}',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ]),
                      const SizedBox(height: 4),
                      Text(
                        'Lat: ${pos.latitude.toStringAsFixed(5)}, '
                        'Lng: ${pos.longitude.toStringAsFixed(5)}',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
