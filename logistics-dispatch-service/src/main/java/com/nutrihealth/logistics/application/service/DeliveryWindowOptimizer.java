package com.nutrihealth.logistics.application.service;

import com.nutrihealth.logistics.domain.model.DeliveryWindow;
import com.nutrihealth.logistics.domain.model.NoAvailableWindowException;
import com.nutrihealth.logistics.domain.port.out.DeliveryWindowRepositoryPort;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Selects the optimal delivery window for a new booking using a
 * load-balancing strategy.
 *
 * <p>Algorithm ({@link Strategy#BALANCED_LOAD}):
 * <ol>
 *   <li>Retrieve all windows for the requested date from the repository.</li>
 *   <li>Filter to windows that still have capacity ({@link DeliveryWindow#hasCapacity()}).</li>
 *   <li>Sort by load factor ascending (least-loaded window first).</li>
 *   <li>Among ties, prefer the earliest start time (customers prefer earlier).</li>
 *   <li>Attempt to atomically book the top candidate.  If the slot is taken
 *       (race condition), fall back to the next candidate.  This retry loop
 *       handles bursts where multiple customers compete for the last slot in
 *       a window simultaneously.</li>
 * </ol>
 *
 * <p>Alternative strategies (future work):
 * <ul>
 *   <li>{@link Strategy#EARLIEST_FIRST} — always pick the earliest window with
 *       capacity, regardless of load.</li>
 *   <li>{@link Strategy#ROUTE_OPTIMIZED} — pick the window whose geographic
 *       cluster minimises total courier travel time (requires courier route data).</li>
 * </ul>
 */
@Service
public class DeliveryWindowOptimizer {

    private static final int MAX_LOOKAHEAD_DAYS = 7;

    public enum Strategy { BALANCED_LOAD, EARLIEST_FIRST }

    private final DeliveryWindowRepositoryPort windowRepository;

    public DeliveryWindowOptimizer(DeliveryWindowRepositoryPort windowRepository) {
        this.windowRepository = windowRepository;
    }

    /**
     * Finds and books the best window for the given date, using
     * {@link Strategy#BALANCED_LOAD} ordering.
     *
     * @param preferredDate the requested delivery date
     * @return the booked {@link DeliveryWindow}
     * @throws NoAvailableWindowException if no window with remaining capacity
     *         exists within {@value #MAX_LOOKAHEAD_DAYS} days of the preferred date
     */
    public DeliveryWindow bookBestWindow(LocalDate preferredDate) {
        return bookBestWindow(preferredDate, Strategy.BALANCED_LOAD);
    }

    /**
     * Finds and books the best window for the given date using the specified strategy.
     *
     * <p>If no window is available on the preferred date, looks ahead up to
     * {@value #MAX_LOOKAHEAD_DAYS} additional days before giving up.
     */
    public DeliveryWindow bookBestWindow(LocalDate preferredDate, Strategy strategy) {
        for (int dayOffset = 0; dayOffset <= MAX_LOOKAHEAD_DAYS; dayOffset++) {
            LocalDate candidate = preferredDate.plusDays(dayOffset);
            Optional<DeliveryWindow> window = selectAndBook(candidate, strategy);
            if (window.isPresent()) {
                return window.get();
            }
        }
        throw new NoAvailableWindowException(
                "No delivery windows available within " + MAX_LOOKAHEAD_DAYS
                        + " days of " + preferredDate);
    }

    /**
     * Returns available windows for a date, sorted by the chosen strategy, without booking.
     */
    public List<DeliveryWindow> getAvailableWindows(LocalDate date) {
        return windowRepository.findWindowsForDate(date).stream()
                .filter(DeliveryWindow::hasCapacity)
                .sorted(Comparator.comparingDouble(DeliveryWindow::loadFactor)
                        .thenComparing(DeliveryWindow::start))
                .toList();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private Optional<DeliveryWindow> selectAndBook(LocalDate date, Strategy strategy) {
        List<DeliveryWindow> candidates = rank(
                windowRepository.findWindowsForDate(date).stream()
                        .filter(DeliveryWindow::hasCapacity)
                        .toList(),
                strategy
        );

        for (DeliveryWindow window : candidates) {
            String key = DeliveryWindowRepositoryPort.windowKey(window);
            boolean booked = windowRepository.bookSlot(date, key);
            if (booked) {
                // Return a view of the window with bookedCount + 1 to reflect the booking.
                return Optional.of(new DeliveryWindow(
                        window.date(), window.start(), window.end(),
                        window.capacity(), window.bookedCount() + 1));
            }
            // Slot was just taken by a concurrent request — try next candidate.
        }
        return Optional.empty();
    }

    private List<DeliveryWindow> rank(List<DeliveryWindow> candidates, Strategy strategy) {
        Comparator<DeliveryWindow> comparator = switch (strategy) {
            case BALANCED_LOAD ->
                    Comparator.comparingDouble(DeliveryWindow::loadFactor)
                              .thenComparing(DeliveryWindow::start);
            case EARLIEST_FIRST ->
                    Comparator.comparing(DeliveryWindow::start);
        };
        return candidates.stream().sorted(comparator).toList();
    }
}
