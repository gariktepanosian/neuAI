# logistics-dispatch-service

Redis geospatial courier dispatch and real-time tracking microservice for
NutriHealth AI.

## Structure (Hexagonal / Ports & Adapters)

```
domain/model         - GeoPoint (Haversine distance), CourierAssignment, NoAvailableCourierException
domain/port/in         - DispatchCourierUseCase (inbound port)
domain/port/out        - CourierLocationRepositoryPort, DeliveryTrackingPublisherPort
application/service    - DispatchService (nearest-available-courier selection)
adapter/in/web          - CourierController (location/availability updates), DispatchController
adapter/out/redis       - RedisCourierLocationAdapter (GEO commands), RedisDeliveryTrackingPublisher (pub/sub)
```

## Dispatch flow

1. Couriers push location updates via `POST /api/v1/couriers/{id}/location`,
   stored with Redis `GEOADD` under the `couriers:geo` key, and republished on
   the `delivery-tracking:courier-location` pub/sub channel for the mobile
   app's live map.
2. Couriers toggle availability via `POST /api/v1/couriers/{id}/available`
   (or `/unavailable`), tracked in the `couriers:available` Redis set —
   separate from location, so a courier keeps their last known position even
   while off-duty.
3. `POST /api/v1/dispatch` runs a Redis `GEORADIUS` search around the
   delivery location (default 10 km, configurable), filters results to the
   `couriers:available` set, and assigns the nearest match — publishing to
   `delivery-tracking:courier-assigned`.

## Running locally

Requires Redis (defaults to `localhost:6379`, override via `REDIS_HOST`/`REDIS_PORT`).

```bash
./mvnw spring-boot:run
```

## Notes / deviations from the spec

- Built against **Java 17**, matching the other services in this repo.
- Availability is modeled as an explicit Redis set that couriers must opt
  into (`/available`) — the spec doesn't define exactly how "available" is
  determined, so this scaffold treats it as separate from "has a recent
  location", which a courier app would update automatically alongside GPS pings.
- No delivery-window optimization algorithm is implemented yet — "delivery
  window optimizations" from the roadmap would need `delivery_schedules`
  data from `subscription-order-service`, which this service doesn't call.
  Currently it only solves nearest-available-courier assignment.
