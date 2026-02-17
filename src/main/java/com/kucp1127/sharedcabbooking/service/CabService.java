package com.kucp1127.sharedcabbooking.service;

import com.kucp1127.sharedcabbooking.domain.entity.Cab;
import com.kucp1127.sharedcabbooking.domain.entity.Location;
import com.kucp1127.sharedcabbooking.domain.enums.CabStatus;
import com.kucp1127.sharedcabbooking.domain.enums.CabType;
import com.kucp1127.sharedcabbooking.dto.mapper.EntityDtoMapper;
import com.kucp1127.sharedcabbooking.dto.request.CabRequest;
import com.kucp1127.sharedcabbooking.dto.response.CabResponse;
import com.kucp1127.sharedcabbooking.exception.ResourceNotFoundException;
import com.kucp1127.sharedcabbooking.repository.CabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for cab management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CabService {

    private final CabRepository cabRepository;
    private final EntityDtoMapper mapper;

    @Transactional
    public CabResponse registerCab(CabRequest request) {
        log.info("Registering cab with license plate: {}", request.getLicensePlate());

        if (cabRepository.findByLicensePlate(request.getLicensePlate()).isPresent()) {
            throw new IllegalArgumentException("Cab already registered: " + request.getLicensePlate());
        }

        Cab cab = mapper.toCab(request);
        cab.setAvailableSeats(cab.getCabType().getMaxPassengers());
        cab.setAvailableLuggageCapacityKg(cab.getCabType().getMaxLuggageWeightKg());
        cab = cabRepository.save(cab);

        log.info("Registered cab with ID: {}", cab.getId());
        return mapper.toCabResponse(cab);
    }

    @Transactional(readOnly = true)
    public CabResponse getCab(Long id) {
        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cab", id));
        return mapper.toCabResponse(cab);
    }

    @Transactional(readOnly = true)
    public CabResponse getCabByLicensePlate(String licensePlate) {
        Cab cab = cabRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new ResourceNotFoundException("Cab", licensePlate));
        return mapper.toCabResponse(cab);
    }

    @Transactional(readOnly = true)
    public List<CabResponse> getAllCabs() {
        return cabRepository.findAll().stream()
                .map(mapper::toCabResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CabResponse> getAvailableCabs() {
        return cabRepository.findByStatus(CabStatus.AVAILABLE).stream()
                .map(mapper::toCabResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CabResponse> getAvailableCabsByType(CabType cabType) {
        return cabRepository.findByCabTypeAndStatus(cabType, CabStatus.AVAILABLE).stream()
                .map(mapper::toCabResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CabResponse> getNearbyCabs(Double latitude, Double longitude, Double radiusKm) {
        return cabRepository.findAvailableCabsNearLocation(latitude, longitude, radiusKm, 20).stream()
                .map(mapper::toCabResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CabResponse updateCabLocation(Long id, Double latitude, Double longitude, String address) {
        log.debug("Updating location for cab: {}", id);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cab", id));

        Location newLocation = Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .address(address)
                .build();

        cab.setCurrentLocation(newLocation);
        cab = cabRepository.save(cab);

        return mapper.toCabResponse(cab);
    }

    @Transactional
    public CabResponse updateCabStatus(Long id, CabStatus status) {
        log.info("Updating status for cab {} to {}", id, status);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cab", id));

        cab.setStatus(status);
        cab = cabRepository.save(cab);

        return mapper.toCabResponse(cab);
    }

    @Transactional
    public void deleteCab(Long id) {
        log.info("Deleting cab: {}", id);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cab", id));

        if (cab.getStatus() != CabStatus.AVAILABLE && cab.getStatus() != CabStatus.OFFLINE) {
            throw new IllegalStateException("Cannot delete cab with active rides");
        }

        cabRepository.deleteById(id);
    }
}
