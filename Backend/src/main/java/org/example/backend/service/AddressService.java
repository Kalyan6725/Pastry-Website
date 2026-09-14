package org.example.backend.service;

import org.example.backend.dto.address.AddressRequest;
import org.example.backend.dto.address.AddressResponse;
import org.example.backend.entity.Address;
import org.example.backend.entity.User;
import org.example.backend.exception.BadRequestException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.AddressRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Address address = Address.builder()
                .user(user)
                .label(request.label())
                .phone(request.phone())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(request.isDefault())
                .build();

        if (request.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(existing -> {
                existing.setDefault(false);
                addressRepository.save(existing);
            });
        }

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByUserIdAndId(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setLabel(request.label());
        address.setPhone(request.phone());
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());

        if (request.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .filter(existing -> !Objects.equals(existing.getId(), addressId))
                    .ifPresent(existing -> existing.setDefault(false));
            address.setDefault(true);
        } else if (address.isDefault()) {
            address.setDefault(false);
        }

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByUserIdAndId(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    public List<AddressResponse> getMyAddresses(Long userId) {
        return addressRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByUserIdAndId(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existing -> {
                    if (!Objects.equals(existing.getId(), addressId)) {
                        existing.setDefault(false);
                        addressRepository.save(existing);
                    }
                });
        address.setDefault(true);
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse unsetDefaultAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByUserIdAndId(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.isDefault()) {
            throw new BadRequestException("Address is not currently the default address");
        }
        address.setDefault(false);
        return toResponse(addressRepository.save(address));
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefault()
        );
    }
}
