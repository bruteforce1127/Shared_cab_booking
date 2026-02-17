-- V2__Initial_Pricing_Data.sql
-- Insert default pricing configuration

-- Base fare configuration
INSERT INTO pricing_configs (config_type, config_key, config_value, numeric_value, description, is_active, priority)
VALUES
    ('BASE_FARE', 'PER_KM_RATE', '15.00', 15.0000, 'Base fare per kilometer in INR', TRUE, 1),
    ('BASE_FARE', 'MINIMUM_FARE', '100.00', 100.0000, 'Minimum fare for any ride', TRUE, 2),
    ('BASE_FARE', 'BOOKING_FEE', '25.00', 25.0000, 'Fixed booking fee', TRUE, 3);

-- Surge pricing thresholds
INSERT INTO pricing_configs (config_type, config_key, config_value, numeric_value, description, is_active, priority)
VALUES
    ('SURGE', 'LOW_DEMAND_THRESHOLD', '50', 50.0000, 'Active requests threshold for low demand', TRUE, 10),
    ('SURGE', 'MEDIUM_DEMAND_THRESHOLD', '100', 100.0000, 'Active requests threshold for medium demand', TRUE, 11),
    ('SURGE', 'HIGH_DEMAND_THRESHOLD', '200', 200.0000, 'Active requests threshold for high demand', TRUE, 12),
    ('SURGE', 'LOW_SURGE_MULTIPLIER', '1.2', 1.2000, 'Surge multiplier for low demand', TRUE, 13),
    ('SURGE', 'MEDIUM_SURGE_MULTIPLIER', '1.5', 1.5000, 'Surge multiplier for medium demand', TRUE, 14),
    ('SURGE', 'HIGH_SURGE_MULTIPLIER', '2.0', 2.0000, 'Surge multiplier for high demand (max)', TRUE, 15);

-- Sharing discount configuration
INSERT INTO pricing_configs (config_type, config_key, config_value, numeric_value, description, is_active, priority)
VALUES
    ('DISCOUNT', 'PER_COPASSENGER_DISCOUNT', '0.05', 0.0500, '5% discount per co-passenger', TRUE, 20),
    ('DISCOUNT', 'MAX_SHARING_DISCOUNT', '0.25', 0.2500, 'Maximum 25% sharing discount', TRUE, 21);

-- Time-based multipliers
INSERT INTO pricing_configs (config_type, config_key, config_value, numeric_value, description, is_active, priority)
VALUES
    ('TIME_MULTIPLIER', 'PEAK_MORNING_START', '06:00', NULL, 'Peak morning start time', TRUE, 30),
    ('TIME_MULTIPLIER', 'PEAK_MORNING_END', '10:00', NULL, 'Peak morning end time', TRUE, 31),
    ('TIME_MULTIPLIER', 'PEAK_EVENING_START', '17:00', NULL, 'Peak evening start time', TRUE, 32),
    ('TIME_MULTIPLIER', 'PEAK_EVENING_END', '21:00', NULL, 'Peak evening end time', TRUE, 33),
    ('TIME_MULTIPLIER', 'PEAK_MULTIPLIER', '1.25', 1.2500, '25% extra during peak hours', TRUE, 34),
    ('TIME_MULTIPLIER', 'NIGHT_MULTIPLIER', '1.5', 1.5000, '50% extra for night rides (10PM-6AM)', TRUE, 35);

-- Cab type multipliers
INSERT INTO pricing_configs (config_type, config_key, config_value, numeric_value, description, is_active, priority)
VALUES
    ('CAB_TYPE', 'SEDAN_MULTIPLIER', '1.0', 1.0000, 'Base rate for Sedan', TRUE, 40),
    ('CAB_TYPE', 'SUV_MULTIPLIER', '1.3', 1.3000, '30% extra for SUV', TRUE, 41),
    ('CAB_TYPE', 'VAN_MULTIPLIER', '1.5', 1.5000, '50% extra for Van', TRUE, 42),
    ('CAB_TYPE', 'PREMIUM_SEDAN_MULTIPLIER', '1.8', 1.8000, '80% extra for Premium Sedan', TRUE, 43);
