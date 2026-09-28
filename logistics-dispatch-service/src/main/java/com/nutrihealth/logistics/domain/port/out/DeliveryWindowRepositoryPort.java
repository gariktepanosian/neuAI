package com.nutrihealth.logistics.domain.port.out;

import com.nutrihealth.logistics.domain.model.DeliveryWindow;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for reading and booking delivery time windows.
 *
 * <p>The primary adapter will back this with Redis (atomic INCR on a slot
 * counter per window key) or Postgres depending on the booking volume.
 */
public interface DeliveryWindowRepositoryPort {

    /**
     * Returns all delivery windows for the given date, with up-to-date
     * booking counts.
     */
    List<DeliveryWindow> findWindowsForDate(LocalDate date);

    /**
     * Atomically increments the booking count for the given window.
     * Returns {@code false} if the window is at capacity (optimistic: the
     * caller should re-query and pick the next best window).
     */
    boolean bookSlot(LocalDate date, String windowKey);

    /**
     * Returns the composite window key used as the Redis hash field /
     * Postgres PK.  Format: {@code <date>/<start>-<end>}
     * e.g. {@code 2026-01-15/08:00-10:00}.
     */
    static String windowKey(DeliveryWindow window) {
        return window.date() + "/" + window.start() + "-" + window.end();
    }
}
