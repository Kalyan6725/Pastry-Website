package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.address.AddressRequest;
import org.example.backend.dto.address.AddressResponse;
import org.example.backend.security.UserPrincipal;
import org.example.backend.service.AddressService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/addresses")
    public List<AddressResponse> getMyAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        return addressService.getMyAddresses(principal.getId());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody AddressRequest request) {
        return addressService.addAddress(principal.getId(), request);
    }

    @PutMapping("/addresses/{id}")
    public AddressResponse updateAddress(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return addressService.updateAddress(principal.getId(), id, request);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        addressService.deleteAddress(principal.getId(), id);
    }

    @PatchMapping("/addresses/{id}/default")
    public AddressResponse setDefault(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return addressService.setDefaultAddress(principal.getId(), id);
    }
}
