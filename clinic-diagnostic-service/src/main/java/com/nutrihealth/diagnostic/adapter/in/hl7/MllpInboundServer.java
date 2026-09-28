package com.nutrihealth.diagnostic.adapter.in.hl7;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * MLLP (Minimum Lower Layer Protocol) TCP server for receiving HL7 v2 ORU^R01
 * messages from clinical lab systems over the standard HL7 wire protocol.
 *
 * <p>MLLP wraps each HL7 message with start ({@code \x0B}) and end
 * ({@code \x1C\x0D}) bytes over a persistent TCP connection.  This is the
 * standard transport used by LIS/EMR systems — it differs from the existing
 * HTTP-over-REST endpoint ({@code POST /hl7v2/oru}) which is used only by
 * modern partners that support REST.
 *
 * <p>Architecture:
 * <pre>
 *   Lab system (LIS)
 *     │  TCP:2575 (MLLP)
 *     ▼
 *   MllpInboundServer (this class)
 *     │  HAPI HL7Service (SimpleServer)
 *     ▼
 *   MllpOruR01Handler  ──► Hl7v2OruTransformer ──► DiagnosticReportService
 * </pre>
 *
 * <p>Port: default {@code 2575} (IANA-assigned for HL7). Override with
 * {@code MLLP_PORT} environment variable.
 *
 * <p>Implements {@link SmartLifecycle} so Spring Boot manages the TCP server
 * lifecycle — starts after the application context is fully initialised and
 * stops cleanly on shutdown.
 */
@Component
public class MllpInboundServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MllpInboundServer.class);

    private final int port;
    private final ReceivingApplication<ca.uhn.hl7v2.model.Message> handler;

    private HapiContext hapiContext;
    private HL7Service hl7Service;
    private volatile boolean running = false;

    public MllpInboundServer(
            @Value("${MLLP_PORT:2575}") int port,
            MllpOruR01Handler handler) {
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void start() {
        hapiContext = new DefaultHapiContext();
        hl7Service = hapiContext.newServer(port, false); // false = not TLS (add TLS termination at load-balancer)

        // Register the ORU^R01 handler for all sending apps / facilities.
        hl7Service.registerApplication("ORU", "R01", handler);

        try {
            hl7Service.startAndWait();
            running = true;
            log.info("MLLP HL7 v2 server listening on TCP port {}", port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("MLLP server start interrupted", e);
        }
    }

    @Override
    @PreDestroy
    public void stop() {
        if (hl7Service != null) {
            hl7Service.stopAndWait();
            running = false;
            log.info("MLLP HL7 v2 server stopped");
        }
        if (hapiContext != null) {
            try {
                hapiContext.close();
            } catch (Exception ex) {
                log.warn("Error closing HAPI context: {}", ex.getMessage());
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Start after normal beans (phase 0) but before the web container.
        return Integer.MAX_VALUE - 100;
    }
}
