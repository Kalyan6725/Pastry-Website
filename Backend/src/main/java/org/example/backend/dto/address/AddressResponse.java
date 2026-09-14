package org.example.backend.dto.address;

public record AddressResponse(
        Long id,
        String label,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault
) {
}
