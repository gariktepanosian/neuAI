# infra/k8s/kafka

Phase 1, Step 1.3: Kafka cluster manifests for the Strimzi operator, per the
spec's choice ("Apache Kafka cluster on K8s using Strimzi operator ... with
dead-letter queue (DLQ) strategy").

**Not applied.** These manifests were written and YAML-validated in this
environment but never run against a real cluster — there is no GKE cluster
yet (see `../terraform`), and applying Kafka manifests without one is
meaningless.

## Layout

- `00-namespace.yaml` — `nutrihealth` (for the microservices) and `kafka`
  (for the Kafka cluster itself) namespaces.
- `10-kafka-cluster.yaml` — the Strimzi `Kafka` custom resource: 3 brokers,
  3 ZooKeeper nodes, replication factor 3 / min.insync.replicas 2 (tolerates
  one broker failure without blocking producers).
- `20-kafka-topics.yaml` — one `KafkaTopic` per business event
  (`subscription.payment.paid`, `subscription.payment.failed`,
  `diagnostic.report.ingested`) plus a matching `<topic>.dlq` topic for each.

## DLQ strategy

Producers (subscription-order-service, clinic-diagnostic-service — see their
`adapter/out/messaging` packages) publish directly to the main topic. Any
future **consumer** should wrap its `@KafkaListener` in Spring Kafka's
`DefaultErrorHandler` configured with a `DeadLetterPublishingRecoverer`
pointed at `<topic>.dlq`, so a message that fails processing after retries
lands on the DLQ topic instead of blocking the partition or being silently
dropped. DLQ topics get 7-day retention vs. 3 days on the source topic, to
give ops time to investigate before a failed message ages out.

No consumer exists yet in this codebase (nothing currently subscribes to
these topics), so the DLQ path is provisioned but unexercised — it activates
once a service starts consuming, e.g. `logistics-dispatch-service` reacting
to `subscription.payment.paid` to schedule a delivery.

## How to actually apply this

```bash
# 1. Install the Strimzi operator (once per cluster)
kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f -
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka

# 2. Wait for the operator to be ready, then apply the cluster + topics
kubectl apply -f 00-namespace.yaml
kubectl apply -f 10-kafka-cluster.yaml
kubectl wait kafka/nutrihealth-kafka --for=condition=Ready --timeout=300s -n kafka
kubectl apply -f 20-kafka-topics.yaml
```

Each microservice's `application.yml` then needs
`spring.kafka.bootstrap-servers` pointed at
`nutrihealth-kafka-kafka-bootstrap.kafka.svc:9092` (Strimzi's generated
bootstrap service) instead of the local-dev default.
