package org.example.backend.dto.order;

import jakarta.validation.constraints.NotNull;

public record OrderRequest(
        @NotNull(message = "Address id is required")
        Long addressId
) {
}
