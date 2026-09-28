package com.nutrihealth.logistics.adapter.in.web;

import com.nutrihealth.logistics.application.service.DeliveryWindowOptimizer;
import com.nutrihealth.logistics.domain.model.DeliveryWindow;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for delivery window management.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/v1/delivery-windows?date=YYYY-MM-DD} — list available windows</li>
 *   <li>{@code POST /api/v1/delivery-windows/book} — book the optimal window for a date</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/delivery-windows")
public class DeliveryWindowController {

    private final DeliveryWindowOptimizer optimizer;

    public DeliveryWindowController(DeliveryWindowOptimizer optimizer) {
        this.optimizer = optimizer;
    }

    /** Returns available (not fully booked) windows for the given date. */
    @GetMapping
    public List<DeliveryWindowResponse> getAvailableWindows(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return optimizer.getAvailableWindows(date).stream()
                .map(DeliveryWindowResponse::from)
                .toList();
    }

    /**
     * Books the optimal window for the given preferred date using load-balancing.
     * Falls back to later dates (up to 7 days) if the preferred date is full.
     */
    @PostMapping("/book")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryWindowResponse bookWindow(@RequestBody BookWindowRequest request) {
        DeliveryWindowOptimizer.Strategy strategy = request.strategy() != null
                ? request.strategy()
                : DeliveryWindowOptimizer.Strategy.BALANCED_LOAD;
        DeliveryWindow booked = optimizer.bookBestWindow(request.preferredDate(), strategy);
        return DeliveryWindowResponse.from(booked);
    }

    public record BookWindowRequest(
            @NotNull LocalDate preferredDate,
            DeliveryWindowOptimizer.Strategy strategy) {
    }

    public record DeliveryWindowResponse(
            LocalDate date,
            String start,
            String end,
            int capacity,
            int bookedCount,
            int remainingCapacity,
            double loadFactor) {

        static DeliveryWindowResponse from(DeliveryWindow w) {
            return new DeliveryWindowResponse(
                    w.date(), w.start().toString(), w.end().toString(),
                    w.capacity(), w.bookedCount(), w.remainingCapacity(), w.loadFactor());
        }
    }
}
