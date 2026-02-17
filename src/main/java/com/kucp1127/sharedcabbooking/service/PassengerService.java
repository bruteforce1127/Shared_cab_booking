package com.kucp1127.sharedcabbooking.service;

import com.kucp1127.sharedcabbooking.domain.entity.Passenger;
import com.kucp1127.sharedcabbooking.dto.mapper.EntityDtoMapper;
import com.kucp1127.sharedcabbooking.dto.request.PassengerRequest;
import com.kucp1127.sharedcabbooking.dto.response.PassengerResponse;
import com.kucp1127.sharedcabbooking.exception.ResourceNotFoundException;
import com.kucp1127.sharedcabbooking.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for passenger management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final EntityDtoMapper mapper;

    @Transactional
    public PassengerResponse createPassenger(PassengerRequest request) {
        log.info("Creating passenger with email: {}", request.getEmail());

        if (passengerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        Passenger passenger = mapper.toPassenger(request);
        passenger = passengerRepository.save(passenger);

        log.info("Created passenger with ID: {}", passenger.getId());
        return mapper.toPassengerResponse(passenger);
    }

    @Transactional(readOnly = true)
    public PassengerResponse getPassenger(Long id) {
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", id));
        return mapper.toPassengerResponse(passenger);
    }

    @Transactional(readOnly = true)
    public PassengerResponse getPassengerByEmail(String email) {
        Passenger passenger = passengerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", email));
        return mapper.toPassengerResponse(passenger);
    }

    @Transactional(readOnly = true)
    public List<PassengerResponse> getAllPassengers() {
        return passengerRepository.findAll().stream()
                .map(mapper::toPassengerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PassengerResponse updatePassenger(Long id, PassengerRequest request) {
        log.info("Updating passenger: {}", id);

        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", id));

        passenger.setName(request.getName());
        passenger.setPhone(request.getPhone());
        passenger.setDetourTolerance(request.getDetourTolerance());
        passenger.setPreferredCabType(request.getPreferredCabType());

        passenger = passengerRepository.save(passenger);
        return mapper.toPassengerResponse(passenger);
    }

    @Transactional
    public void deletePassenger(Long id) {
        log.info("Deleting passenger: {}", id);

        if (!passengerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Passenger", id);
        }

        passengerRepository.deleteById(id);
    }
}
