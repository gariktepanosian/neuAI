package com.nutrihealth.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.port.out.SubscriptionRepositoryPort;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for the subscription payment flow:
 * HTTP request → domain → Postgres persistence → Kafka event publishing.
 *
 * <p>Infrastructure: Testcontainers spins up real Postgres and Kafka
 * containers for the test. No mocks are used for infrastructure — this
 * validates the full wiring.
 *
 * <p>Stripe: Webhook signature verification is bypassed in the test by
 * using the {@code StripeWebhookControllerTest} signing approach (real HMAC
 * with a known test secret). No live Stripe API calls.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class SubscriptionPaymentIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("subscription_db")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // Use a recognisable test topic so we know what to consume.
        registry.add("nutrihealth.kafka.topic.subscription-paid", () -> "test.subscription.payment.paid");
        registry.add("nutrihealth.kafka.topic.subscription-failed", () -> "test.subscription.payment.failed");
        // Skip real Stripe verification — use a static test secret.
        registry.add("nutrihealth.stripe.webhook-signing-secret", () -> "whsec_test_secret_for_integration");
        registry.add("nutrihealth.stripe.api-key", () -> "sk_test_placeholder");
    }

    @Autowired MockMvc mockMvc;
    @Autowired SubscriptionRepositoryPort subscriptionRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createSubscriptionAndVerifyPersisted() throws Exception {
        // POST /api/v1/subscriptions — creates a new subscription
        String createPayload = objectMapper.writeValueAsString(Map.of(
                "userId", "550e8400-e29b-41d4-a716-446655440000",
                "stripeSubscriptionId", "sub_test_12345",
                "scheduleType", "WEEKDAY_5_DAY"
        ));

        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());

        // Verify the subscription was persisted in Postgres.
        List<Subscription> found = subscriptionRepository.findByUserId(
                java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getStripeSubscriptionId()).isEqualTo("sub_test_12345");
    }

    @Test
    void stripeWebhookPublishesKafkaEventOnPaymentSucceeded() throws Exception {
        // First create a subscription to update.
        String subId = "sub_kafka_test_001";
        String createPayload = objectMapper.writeValueAsString(Map.of(
                "userId", "660e8400-e29b-41d4-a716-446655440001",
                "stripeSubscriptionId", subId,
                "scheduleType", "FULL_7_DAY"
        ));
        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());

        // Construct a real Stripe invoice.payment_succeeded event payload.
        String webhookPayload = buildStripeWebhookPayload(subId, "invoice.payment_succeeded");
        String sigHeader = buildStripeSignatureHeader(webhookPayload, "whsec_test_secret_for_integration");

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(webhookPayload))
                .andExpect(status().isOk());

        // Consume from Kafka and verify the event was published.
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "integration-test-consumer");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of("test.subscription.payment.paid"));
            var records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);

            ConsumerRecord<String, String> record = records.iterator().next();
            Map<?, ?> event = objectMapper.readValue(record.value(), Map.class);
            assertThat(event.get("eventType")).isEqualTo("subscription.payment.paid");
            assertThat(event.get("stripeSubscriptionId")).isEqualTo(subId);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String buildStripeWebhookPayload(String stripeSubId, String eventType) {
        return """
                {
                  "type": "%s",
                  "data": {
                    "object": {
                      "id": "in_test_001",
                      "status": "paid",
                      "parent": {
                        "subscription_details": {
                          "subscription": "%s"
                        }
                      }
                    }
                  }
                }
                """.formatted(eventType, stripeSubId);
    }

    private static String buildStripeSignatureHeader(String payload, String secret) throws Exception {
        long ts = System.currentTimeMillis() / 1000;
        String signedPayload = ts + "." + payload;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                secret.replace("whsec_", "").getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"));
        byte[] hmac = mac.doFinal(signedPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String sig = bytesToHex(hmac);
        return "t=" + ts + ",v1=" + sig;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
