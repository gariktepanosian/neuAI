package com.nutrihealth.logistics.adapter.in.web;

import com.nutrihealth.logistics.domain.model.GeoPoint;
import com.nutrihealth.logistics.domain.port.in.DispatchCourierUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couriers")
public class CourierController {

    private final DispatchCourierUseCase useCase;

    public CourierController(DispatchCourierUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/{courierId}/location")
    public void updateLocation(@PathVariable UUID courierId, @Valid @RequestBody LocationRequest request) {
        useCase.recordCourierLocation(courierId, new GeoPoint(request.latitude(), request.longitude()));
    }

    @PostMapping("/{courierId}/available")
    public void markAvailable(@PathVariable UUID courierId) {
        useCase.markCourierAvailable(courierId);
    }

    @PostMapping("/{courierId}/unavailable")
    public void markUnavailable(@PathVariable UUID courierId) {
        useCase.markCourierUnavailable(courierId);
    }

    public record LocationRequest(@NotNull Double latitude, @NotNull Double longitude) {
    }
}
