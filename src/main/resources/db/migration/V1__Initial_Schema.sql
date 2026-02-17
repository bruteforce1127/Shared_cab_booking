-- V1__Initial_Schema.sql
-- Smart Airport Ride Pooling System - Initial Database Schema

-- Passengers table
CREATE TABLE passengers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50) NOT NULL,
    detour_tolerance DOUBLE PRECISION DEFAULT 0.20,
    preferred_cab_type VARCHAR(50),
    rating DOUBLE PRECISION DEFAULT 5.0,
    total_rides INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_passenger_email ON passengers(email);
CREATE INDEX idx_passenger_phone ON passengers(phone);

-- Cabs table
CREATE TABLE cabs (
    id BIGSERIAL PRIMARY KEY,
    license_plate VARCHAR(50) NOT NULL UNIQUE,
    driver_name VARCHAR(255) NOT NULL,
    driver_phone VARCHAR(50) NOT NULL,
    cab_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    current_latitude DOUBLE PRECISION,
    current_longitude DOUBLE PRECISION,
    current_address TEXT,
    driver_rating DOUBLE PRECISION DEFAULT 5.0,
    available_seats INTEGER,
    available_luggage_capacity_kg DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_cab_status ON cabs(status);
CREATE INDEX idx_cab_type_status ON cabs(cab_type, status);
CREATE INDEX idx_cab_license ON cabs(license_plate);

-- Ride Groups table (core pooling entity)
CREATE TABLE ride_groups (
    id BIGSERIAL PRIMARY KEY,
    cab_id BIGINT REFERENCES cabs(id),
    status VARCHAR(50) NOT NULL DEFAULT 'FORMING',
    airport_latitude DOUBLE PRECISION,
    airport_longitude DOUBLE PRECISION,
    airport_address TEXT,
    scheduled_departure_time TIMESTAMP,
    actual_departure_time TIMESTAMP,
    estimated_arrival_time TIMESTAMP,
    total_passengers INTEGER DEFAULT 0,
    total_luggage_weight_kg DOUBLE PRECISION DEFAULT 0.0,
    optimized_route TEXT,
    total_distance_km DOUBLE PRECISION,
    direct_distance_km DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_ride_group_status ON ride_groups(status);
CREATE INDEX idx_ride_group_status_created ON ride_groups(status, created_at);
CREATE INDEX idx_ride_group_departure_time ON ride_groups(scheduled_departure_time);
CREATE INDEX idx_ride_group_cab ON ride_groups(cab_id);

-- Bookings table (main transactional entity)
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    passenger_id BIGINT NOT NULL REFERENCES passengers(id),
    ride_group_id BIGINT REFERENCES ride_groups(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    pickup_latitude DOUBLE PRECISION,
    pickup_longitude DOUBLE PRECISION,
    pickup_address TEXT,
    dropoff_latitude DOUBLE PRECISION,
    dropoff_longitude DOUBLE PRECISION,
    dropoff_address TEXT,
    requested_pickup_time TIMESTAMP NOT NULL,
    estimated_pickup_time TIMESTAMP,
    actual_pickup_time TIMESTAMP,
    passenger_count INTEGER NOT NULL DEFAULT 1,
    luggage_weight_kg DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    luggage_count INTEGER NOT NULL DEFAULT 0,
    max_detour_tolerance DOUBLE PRECISION DEFAULT 0.20,
    direct_distance_km DOUBLE PRECISION,
    actual_distance_km DOUBLE PRECISION,
    base_fare DECIMAL(10,2),
    final_fare DECIMAL(10,2),
    sharing_discount DECIMAL(10,2) DEFAULT 0.00,
    surge_multiplier DOUBLE PRECISION DEFAULT 1.0,
    special_requirements TEXT,
    pickup_sequence INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_booking_status ON bookings(status);
CREATE INDEX idx_booking_passenger_status ON bookings(passenger_id, status);
CREATE INDEX idx_booking_pickup_time ON bookings(requested_pickup_time);
CREATE INDEX idx_booking_ride_group ON bookings(ride_group_id);
CREATE INDEX idx_booking_created_at ON bookings(created_at);
-- Composite index for ride matching queries
CREATE INDEX idx_booking_matching ON bookings(status, requested_pickup_time, pickup_latitude, pickup_longitude);

-- Cancellations table (audit trail)
CREATE TABLE cancellations (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    cancelled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,
    initiated_by VARCHAR(50) NOT NULL,
    cancellation_fee DECIMAL(10,2) DEFAULT 0.00,
    refund_amount DECIMAL(10,2) DEFAULT 0.00,
    triggered_rebalance BOOLEAN DEFAULT FALSE,
    affected_ride_group_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_cancellation_booking ON cancellations(booking_id);
CREATE INDEX idx_cancellation_created_at ON cancellations(created_at);

-- Pricing Configuration table
CREATE TABLE pricing_configs (
    id BIGSERIAL PRIMARY KEY,
    config_type VARCHAR(50) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    numeric_value DECIMAL(10,4),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_pricing_config_active ON pricing_configs(is_active);
CREATE INDEX idx_pricing_config_type ON pricing_configs(config_type);
CREATE UNIQUE INDEX idx_pricing_config_key ON pricing_configs(config_type, config_key) WHERE is_active = TRUE;
