package com.nutrihealth.logistics.adapter.in.web;

import com.nutrihealth.logistics.domain.model.CourierAssignment;
import com.nutrihealth.logistics.domain.model.GeoPoint;
import com.nutrihealth.logistics.domain.port.in.DispatchCourierUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dispatch")
public class DispatchController {

    private final DispatchCourierUseCase useCase;

    public DispatchController(DispatchCourierUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public CourierAssignment dispatch(@Valid @RequestBody DispatchRequest request) {
        return useCase.dispatchNearestCourier(request.deliveryId(),
                new GeoPoint(request.latitude(), request.longitude()));
    }

    public record DispatchRequest(@NotNull UUID deliveryId, @NotNull Double latitude, @NotNull Double longitude) {
    }
}
