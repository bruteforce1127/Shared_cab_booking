package com.kucp1127.sharedcabbooking.dto.mapper;

import com.kucp1127.sharedcabbooking.domain.entity.*;
import com.kucp1127.sharedcabbooking.dto.request.*;
import com.kucp1127.sharedcabbooking.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityDtoMapper {

    // ==================== PASSENGER MAPPINGS ====================

    public Passenger toPassenger(PassengerRequest request) {
        return Passenger.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .detourTolerance(request.getDetourTolerance())
                .preferredCabType(request.getPreferredCabType())
                .build();
    }

    public PassengerResponse toPassengerResponse(Passenger passenger) {
        return PassengerResponse.builder()
                .id(passenger.getId())
                .name(passenger.getName())
                .email(passenger.getEmail())
                .phone(passenger.getPhone())
                .detourTolerance(passenger.getDetourTolerance())
                .preferredCabType(passenger.getPreferredCabType())
                .rating(passenger.getRating())
                .totalRides(passenger.getTotalRides())
                .build();
    }

    // ==================== CAB MAPPINGS ====================

    public Cab toCab(CabRequest request) {
        Location currentLocation = null;
        if (request.getCurrentLatitude() != null && request.getCurrentLongitude() != null) {
            currentLocation = Location.builder()
                    .latitude(request.getCurrentLatitude())
                    .longitude(request.getCurrentLongitude())
                    .address(request.getCurrentAddress())
                    .build();
        }

        return Cab.builder()
                .licensePlate(request.getLicensePlate())
                .driverName(request.getDriverName())
                .driverPhone(request.getDriverPhone())
                .cabType(request.getCabType())
                .currentLocation(currentLocation)
                .build();
    }

    public CabResponse toCabResponse(Cab cab) {
        CabResponse.CabResponseBuilder builder = CabResponse.builder()
                .id(cab.getId())
                .licensePlate(cab.getLicensePlate())
                .driverName(cab.getDriverName())
                .driverPhone(cab.getDriverPhone())
                .cabType(cab.getCabType())
                .status(cab.getStatus())
                .driverRating(cab.getDriverRating())
                .availableSeats(cab.getAvailableSeats())
                .availableLuggageCapacityKg(cab.getAvailableLuggageCapacityKg());

        if (cab.getCurrentLocation() != null) {
            builder.currentLatitude(cab.getCurrentLocation().getLatitude())
                    .currentLongitude(cab.getCurrentLocation().getLongitude())
                    .currentAddress(cab.getCurrentLocation().getAddress());
        }

        return builder.build();
    }

    // ==================== BOOKING/RIDE MAPPINGS ====================

    public RideResponse toRideResponse(Booking booking) {
        RideResponse.RideResponseBuilder builder = RideResponse.builder()
                .bookingId(booking.getId())
                .passengerId(booking.getPassenger().getId())
                .status(booking.getStatus())
                .requestedPickupTime(booking.getRequestedPickupTime())
                .estimatedPickupTime(booking.getEstimatedPickupTime())
                .passengerCount(booking.getPassengerCount())
                .luggageWeightKg(booking.getLuggageWeightKg())
                .luggageCount(booking.getLuggageCount())
                .pickupSequence(booking.getPickupSequence())
                .estimatedFare(booking.getFinalFare())
                .sharingDiscount(booking.getSharingDiscount())
                .surgeMultiplier(booking.getSurgeMultiplier())
                .createdAt(booking.getCreatedAt());

        // Map pickup location
        if (booking.getPickupLocation() != null) {
            builder.pickupLocation(toLocationDto(booking.getPickupLocation()));
        }

        // Map dropoff location
        if (booking.getDropoffLocation() != null) {
            builder.dropoffLocation(toLocationDto(booking.getDropoffLocation()));
        }

        // Map ride group info
        if (booking.getRideGroup() != null) {
            RideGroup group = booking.getRideGroup();
            builder.rideGroupId(group.getId())
                    .estimatedArrivalTime(group.getEstimatedArrivalTime())
                    .totalCoPassengers(group.getTotalPassengers() - booking.getPassengerCount())
                    .estimatedDetourPercentage(group.getDetourPercentage());

            // Map cab info
            if (group.getCab() != null) {
                builder.cabInfo(toCabInfoDto(group.getCab()));
            }
        }

        return builder.build();
    }

    public RideResponse.LocationDto toLocationDto(Location location) {
        return RideResponse.LocationDto.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .address(location.getAddress())
                .build();
    }

    public RideResponse.CabInfoDto toCabInfoDto(Cab cab) {
        return RideResponse.CabInfoDto.builder()
                .licensePlate(cab.getLicensePlate())
                .driverName(cab.getDriverName())
                .driverPhone(cab.getDriverPhone())
                .cabType(cab.getCabType().name())
                .driverRating(cab.getDriverRating())
                .build();
    }

    // ==================== RIDE GROUP MAPPINGS ====================

    public RideGroupResponse toRideGroupResponse(RideGroup group) {
        RideGroupResponse.RideGroupResponseBuilder builder = RideGroupResponse.builder()
                .id(group.getId())
                .status(group.getStatus())
                .scheduledDepartureTime(group.getScheduledDepartureTime())
                .estimatedArrivalTime(group.getEstimatedArrivalTime())
                .totalPassengers(group.getTotalPassengers())
                .totalLuggageWeightKg(group.getTotalLuggageWeightKg())
                .totalDistanceKm(group.getTotalDistanceKm())
                .detourPercentage(group.getDetourPercentage());

        // Map airport location
        if (group.getAirportLocation() != null) {
            builder.airportLocation(toLocationDto(group.getAirportLocation()));
        }

        // Map cab info
        if (group.getCab() != null) {
            builder.cabInfo(toCabInfoDto(group.getCab()));
        }

        // Map booking summaries
        if (group.getBookings() != null && !group.getBookings().isEmpty()) {
            List<RideGroupResponse.BookingSummary> summaries = group.getBookings().stream()
                    .map(this::toBookingSummary)
                    .collect(Collectors.toList());
            builder.bookings(summaries);
        }

        return builder.build();
    }

    private RideGroupResponse.BookingSummary toBookingSummary(Booking booking) {
        return RideGroupResponse.BookingSummary.builder()
                .bookingId(booking.getId())
                .passengerId(booking.getPassenger().getId())
                .passengerName(booking.getPassenger().getName())
                .pickupSequence(booking.getPickupSequence())
                .pickupLocation(toLocationDto(booking.getPickupLocation()))
                .estimatedPickupTime(booking.getEstimatedPickupTime())
                .build();
    }
}
