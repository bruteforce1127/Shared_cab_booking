# Smart Airport Ride Pooling Backend System

## Overview

A Spring Boot backend system that groups passengers into shared cabs while optimizing routes and pricing for airport rides.

## Architecture

### High Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         API Layer (REST)                            │
│    ┌──────────────┐ ┌───────────────┐ ┌────────────┐ ┌──────────┐  │
│    │ RideController│ │PassengerCtrl  │ │ CabCtrl    │ │PricingCtrl│ │
│    └──────────────┘ └───────────────┘ └────────────┘ └──────────┘  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                         Service Layer                               │
│  ┌─────────────────┐  ┌──────────────┐  ┌─────────────────────────┐│
│  │ RideGrouping    │  │ Cancellation │  │   Pricing Engine        ││
│  │ Service         │  │ Service      │  │   (Chain of Resp.)      ││
│  │ (Strategy)      │  │ (Observer)   │  └─────────────────────────┘│
│  └─────────────────┘  └──────────────┘                              │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │              Distributed Lock Service (Redisson)                ││
│  └─────────────────────────────────────────────────────────────────┘│
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                      Repository Layer (JPA)                         │
│  ┌──────────┐ ┌───────────┐ ┌────────────┐ ┌───────────────────────┐│
│  │Passenger │ │ Booking   │ │ RideGroup  │ │ Cab / PricingConfig   ││
│  │Repository│ │ Repository│ │ Repository │ │ Repositories          ││
│  └──────────┘ └───────────┘ └────────────┘ └───────────────────────┘│
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                      Data Layer                                      │
│    ┌──────────────────────┐         ┌──────────────────────┐        │
│    │     PostgreSQL       │         │       Redis          │        │
│    │  (Primary Database)  │         │  (Cache + Locks)     │        │
│    └──────────────────────┘         └──────────────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
```

## Design Patterns Used

### 1. Strategy Pattern
**Location:** `service/matching/`

Used for ride matching algorithms. Allows switching between different matching strategies at runtime.

```
RideMatchingStrategy (Interface)
├── GreedyNearestNeighborStrategy (Primary)
└── ConstraintBasedClusteringStrategy (Fallback)
```

### 2. Chain of Responsibility Pattern
**Location:** `service/pricing/`

Used for pricing calculation. Each pricing modifier applies in sequence.

```
PricingStrategy (Interface)
├── BaseFarePricingStrategy (Priority: 1)
├── SurgePricingStrategy (Priority: 2)
├── CabTypeMultiplierStrategy (Priority: 3)
└── SharedRideDiscountStrategy (Priority: 10)
```

**Pricing Formula:**
```
FinalPrice = (BookingFee + DistanceKm × PerKmRate) × SurgeMultiplier × CabTypeMultiplier × (1 - SharingDiscount)
```

### 3. Observer Pattern
**Location:** `event/`, `service/booking/`

Used for real-time cancellation handling and group rebalancing.

```
BookingCancelledEvent → RideGroupRebalancer (Async Listener)
```

### 4. Repository Pattern
**Location:** `repository/`

Standard Spring Data JPA repositories with custom queries for geo-proximity searches.

### 5. Builder Pattern
**Location:** All DTOs and Entities

Using Lombok `@Builder` for flexible object construction.

### 6. Facade Pattern
**Location:** `controller/`

Controllers act as facades, hiding service layer complexity from API consumers.

## DSA Approaches

### 1. Ride Matching (Spatial + Constraint-Based)
- **Algorithm:** Greedy Nearest Neighbor with spatial filtering
- **Complexity:** O(n log n) with R-tree spatial index
- **Steps:**
  1. Filter by proximity using Haversine distance
  2. Filter by time window
  3. Check capacity constraints (seats, luggage)
  4. Calculate detour impact
  5. Score and rank candidates

### 2. Route Optimization (TSP Approximation)
- **Algorithm:** Nearest Neighbor Heuristic
- **Complexity:** O(n²) where n = number of stops
- **Use Case:** Optimize pickup sequence within a ride group

### 3. Distance Calculation
- **Algorithm:** Haversine Formula
- **Complexity:** O(1) per calculation
- **Accuracy:** ~0.5% error margin

## Database Schema

### Core Tables
| Table | Purpose | Key Indexes |
|-------|---------|-------------|
| `passengers` | User/passenger data | `email` (unique), `phone` |
| `cabs` | Cab/driver data | `status`, `cab_type, status` |
| `ride_groups` | Pooled ride groups | `status, created_at` |
| `bookings` | Individual bookings | `passenger_id, status`, `ride_group_id` |
| `cancellations` | Cancellation audit trail | `booking_id` |
| `pricing_configs` | Dynamic pricing config | `config_type, is_active` |

### Indexing Strategy
- **B-tree indexes** on status and timestamp columns for filtering
- **Composite indexes** for common query patterns
- **Haversine-based spatial queries** (PostGIS can be added for production)

## Concurrency Handling

### 1. Distributed Locking (Redis/Redisson)
- Lock TTL: 10 seconds
- Wait time: 5 seconds
- Lock types: Ride Group, Booking, Cab

### 2. Database Locking
- **Optimistic locking:** `@Version` on entities
- **Pessimistic locking:** `@Lock(PESSIMISTIC_WRITE)` for critical sections

### 3. Transaction Isolation
- **REPEATABLE_READ:** For booking confirmations
- **READ_COMMITTED:** For read operations

## Performance Optimizations

### Caching (Redis)
| Cache | TTL | Purpose |
|-------|-----|---------|
| `pricingConfig` | 1 hour | Rarely changing config |
| `surgeMultiplier` | 30 seconds | Fresh demand data |
| `cabAvailability` | 1 minute | Near-real-time |

### Connection Pooling (HikariCP)
- Max pool size: 50
- Min idle: 10
- Idle timeout: 5 minutes

### Async Processing
- Thread pools for non-critical operations
- Event-driven rebalancing

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/passengers` | POST/GET/PUT/DELETE | Passenger CRUD |
| `/api/v1/cabs` | POST/GET/PATCH/DELETE | Cab management |
| `/api/v1/rides` | POST | Book a ride |
| `/api/v1/rides/{id}` | GET | Get ride details |
| `/api/v1/bookings/cancel` | POST | Cancel booking |
| `/api/v1/pricing/estimate` | POST | Get fare estimate |
| `/api/v1/pricing/surge` | GET | Current surge info |

## Running the Application

### Prerequisites
- Java 17+
- PostgreSQL 14+
- Redis 6+
- Maven 3.8+

### Configuration
Update `application.properties` with your database and Redis connection details.

### Start Application
```bash
mvn spring-boot:run
```

### Access Swagger UI
```
http://localhost:8080/swagger-ui.html
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Scaling Considerations

1. **Horizontal Scaling:** Stateless services, Redis for distributed state
2. **Database Sharding:** By region/airport
3. **Read Replicas:** For read-heavy operations
4. **Message Queue:** Add Kafka/RabbitMQ for event processing at scale
