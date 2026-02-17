# 🚕 Smart Airport Ride Pooling System

A robust Spring Boot backend system that intelligently groups passengers into shared cabs, optimizing routes and pricing for airport rides.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![Redis](https://img.shields.io/badge/Redis-7-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Design Patterns](#-design-patterns)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Installation & Setup](#-installation--setup)
- [Running the Application](#-running-the-application)
- [API Documentation](#-api-documentation)
- [API Endpoints](#-api-endpoints)
- [Testing Flow](#-testing-flow)
- [Sample Data](#-sample-data)
- [Pricing Model](#-pricing-model)
- [Error Handling](#-error-handling)
- [Project Structure](#-project-structure)

---

## 🎯 Overview

This system provides an intelligent ride-pooling solution for airport transfers. It matches passengers with similar routes and timing preferences, assigns optimal cabs, calculates dynamic pricing with sharing discounts, and handles cancellations with automatic group rebalancing.

### Key Capabilities

- **Smart Ride Matching**: Groups passengers based on proximity, time windows, and route compatibility
- **Dynamic Pricing**: Surge pricing, cab type multipliers, and sharing discounts
- **Real-time Updates**: Distributed locking for concurrent booking handling
- **Cancellation Management**: Automatic refunds and group rebalancing
- **Scalable Architecture**: Redis caching and connection pooling

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **Passenger Management** | Register, update, and manage passenger profiles |
| **Cab Fleet Management** | Register cabs, track locations, manage availability |
| **Intelligent Ride Matching** | Match passengers using spatial algorithms |
| **Dynamic Pricing Engine** | Calculate fares with surge, discounts, and cab type multipliers |
| **Ride Group Management** | Create and manage shared ride groups |
| **Booking Cancellation** | Cancel bookings with automatic refund calculation |
| **Real-time Status Tracking** | Track booking and ride group status |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         API Layer (REST)                            │
│    ┌──────────────┐ ┌───────────────┐ ┌────────────┐ ┌──────────┐  │
│    │RideController│ │PassengerCtrl  │ │ CabCtrl    │ │PricingCtrl│ │
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

---

## 🎨 Design Patterns

### 1. Strategy Pattern
**Location:** `service/matching/`

Used for ride matching algorithms with interchangeable strategies:
- `GreedyNearestNeighborStrategy` - Primary matching algorithm
- `ConstraintBasedClusteringStrategy` - Fallback strategy

### 2. Chain of Responsibility Pattern
**Location:** `service/pricing/`

Pricing calculation with chained modifiers:
```
BaseFarePricingStrategy → SurgePricingStrategy → CabTypeMultiplierStrategy → SharedRideDiscountStrategy
```

### 3. Observer Pattern
**Location:** `event/`, `service/booking/`

Event-driven cancellation handling:
```
BookingCancelledEvent → RideGroupRebalancer (Async Listener)
```

### 4. Repository Pattern
Standard Spring Data JPA repositories with custom geo-proximity queries.

### 5. Builder Pattern
All DTOs and Entities use Lombok `@Builder` for flexible construction.

### 6. Facade Pattern
Controllers act as facades, hiding service complexity from API consumers.

---

## 🛠 Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java 21** | Programming Language |
| **Spring Boot 3.4.2** | Application Framework |
| **Spring Data JPA** | Database ORM |
| **PostgreSQL 17** | Primary Database |
| **Redis 7** | Caching & Distributed Locks |
| **Redisson** | Redis Client for Distributed Locking |
| **Flyway** | Database Migrations |
| **Lombok** | Boilerplate Reduction |
| **SpringDoc OpenAPI** | API Documentation |
| **HikariCP** | Connection Pooling |

---

## 📦 Prerequisites

Before running the application, ensure you have:

- **Java 21** or higher
- **Maven 3.8+**
- **PostgreSQL 14+** (running on port 5432)
- **Redis 6+** (running on port 6379)

---

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/shared-cab-booking.git
cd shared-cab-booking
```

### 2. Set Up PostgreSQL Database

```sql
-- Connect to PostgreSQL and create the database
CREATE DATABASE shared_cab_booking;
```

### 3. Set Up Redis

**Windows (using Docker):**
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

**Windows (using Chocolatey):**
```bash
choco install redis-64
redis-server
```

**Linux/Mac:**
```bash
# Ubuntu/Debian
sudo apt install redis-server
sudo systemctl start redis

# Mac
brew install redis
brew services start redis
```

### 4. Configure Application

Update `src/main/resources/application.properties` with your database credentials:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/shared_cab_booking
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 5. Build the Application

```bash
mvn clean install -DskipTests
```

---

## ▶ Running the Application

### Using Maven

```bash
mvn spring-boot:run
```

### Using Java JAR

```bash
java -jar target/shared-cab-booking-0.0.1-SNAPSHOT.jar
```

### Verify Application is Running

Open your browser and navigate to:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/v3/api-docs

---

## 📚 API Documentation

### Swagger UI

Access the interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

### Base URL
```
http://localhost:8080
```

---

## 🔗 API Endpoints

### 1. Passenger Management (`/passengers`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/passengers` | Register a new passenger |
| `GET` | `/passengers` | Get all passengers |
| `GET` | `/passengers/{id}` | Get passenger by ID |
| `GET` | `/passengers/email/{email}` | Get passenger by email |
| `PUT` | `/passengers/{id}` | Update passenger |
| `DELETE` | `/passengers/{id}` | Delete passenger |

### 2. Cab Management (`/cabs`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/cabs` | Register a new cab |
| `GET` | `/cabs` | Get all cabs |
| `GET` | `/cabs/{id}` | Get cab by ID |
| `GET` | `/cabs/license/{licensePlate}` | Get cab by license plate |
| `GET` | `/cabs/available` | Get available cabs |
| `GET` | `/cabs/available/type/{cabType}` | Get available cabs by type |
| `GET` | `/cabs/nearby` | Get nearby cabs |
| `PATCH` | `/cabs/{id}/location` | Update cab location |
| `PATCH` | `/cabs/{id}/status` | Update cab status |
| `DELETE` | `/cabs/{id}` | Delete cab |

### 3. Pricing (`/pricing`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/pricing/estimate` | Get fare estimate |
| `GET` | `/pricing/surge` | Get current surge multiplier |

### 4. Rides (`/rides`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/rides` | Book a new ride |
| `GET` | `/rides/{bookingId}` | Get ride details |
| `GET` | `/rides/passenger/{passengerId}` | Get passenger's rides |
| `GET` | `/rides/passenger/{passengerId}/paginated` | Get rides (paginated) |
| `GET` | `/rides/passenger/{passengerId}/active` | Get active rides |
| `GET` | `/rides/groups/{groupId}` | Get ride group details |
| `GET` | `/rides/{bookingId}/group` | Get ride group for booking |

### 5. Bookings (`/bookings`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/bookings/cancel` | Cancel a booking |
| `GET` | `/bookings/{bookingId}/can-cancel` | Check if booking can be cancelled |

---

## 🧪 Testing Flow

Follow this order to test the complete ride pooling flow:

### Step 1: Register a Passenger

```bash
curl -X POST http://localhost:8080/passengers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+919876543210",
    "detourTolerance": 0.20,
    "preferredCabType": "SEDAN"
  }'
```

### Step 2: Register a Cab

```bash
curl -X POST http://localhost:8080/cabs \
  -H "Content-Type: application/json" \
  -d '{
    "licensePlate": "DL-01-AB-1234",
    "driverName": "Rajesh Kumar",
    "driverPhone": "+919876543220",
    "cabType": "SEDAN",
    "currentLatitude": 28.6139,
    "currentLongitude": 77.2090,
    "currentAddress": "Connaught Place, New Delhi"
  }'
```

### Step 3: Get Fare Estimate

```bash
curl -X POST http://localhost:8080/pricing/estimate \
  -H "Content-Type: application/json" \
  -d '{
    "passengerId": 1,
    "pickupLatitude": 28.6139,
    "pickupLongitude": 77.2090,
    "dropoffLatitude": 28.5562,
    "dropoffLongitude": 77.1000,
    "requestedPickupTime": "2026-02-20T14:00:00",
    "passengerCount": 1,
    "luggageWeightKg": 15.0,
    "preferredCabType": "SEDAN"
  }'
```

### Step 4: Book a Ride

```bash
curl -X POST http://localhost:8080/rides \
  -H "Content-Type: application/json" \
  -d '{
    "passengerId": 1,
    "pickupLatitude": 28.6139,
    "pickupLongitude": 77.2090,
    "pickupAddress": "Connaught Place, New Delhi",
    "dropoffLatitude": 28.5562,
    "dropoffLongitude": 77.1000,
    "dropoffAddress": "IGI Airport Terminal 3",
    "requestedPickupTime": "2026-02-20T14:00:00",
    "passengerCount": 1,
    "luggageWeightKg": 15.0,
    "luggageCount": 2,
    "maxDetourTolerance": 0.20,
    "preferredCabType": "SEDAN"
  }'
```

### Step 5: Check Ride Details

```bash
curl http://localhost:8080/rides/1
```

### Step 6: Cancel Booking (Optional)

```bash
curl -X POST http://localhost:8080/bookings/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 1,
    "reason": "Change of plans",
    "initiatedBy": "PASSENGER"
  }'
```

---

## 📍 Sample Data

### Cab Types

| Type | Max Passengers | Max Luggage | Max Weight | Multiplier |
|------|----------------|-------------|------------|------------|
| `SEDAN` | 4 | 3 pieces | 100 kg | 1.0x |
| `SUV` | 6 | 5 pieces | 150 kg | 1.3x |
| `VAN` | 8 | 8 pieces | 200 kg | 1.5x |
| `PREMIUM_SEDAN` | 4 | 3 pieces | 100 kg | 1.8x |

### Cab Statuses

| Status | Description |
|--------|-------------|
| `AVAILABLE` | Ready for new rides |
| `ASSIGNED` | Assigned to a ride group |
| `EN_ROUTE` | En route to pickup |
| `ON_TRIP` | Currently on a trip |
| `OFFLINE` | Not available |

### Booking Statuses

| Status | Description |
|--------|-------------|
| `PENDING` | Booking created, awaiting confirmation |
| `CONFIRMED` | Booking confirmed |
| `IN_PROGRESS` | Ride in progress |
| `COMPLETED` | Ride completed |
| `CANCELLED` | Booking cancelled |
| `EXPIRED` | Booking expired |

### Sample Locations (Delhi NCR)

| Location | Latitude | Longitude |
|----------|----------|-----------|
| Connaught Place | 28.6139 | 77.2090 |
| India Gate | 28.6129 | 77.2295 |
| IGI Airport T3 | 28.5562 | 77.1000 |
| Noida Sector 18 | 28.5700 | 77.3200 |
| Gurgaon Cyber Hub | 28.4950 | 77.0880 |
| Karol Bagh | 28.6514 | 77.1907 |
| Nehru Place | 28.5491 | 77.2533 |
| Saket | 28.5244 | 77.2066 |

---

## 💰 Pricing Model

### Formula

```
FinalPrice = (BookingFee + DistanceKm × PerKmRate) × SurgeMultiplier × CabTypeMultiplier × (1 - SharingDiscount)
```

### Default Values

| Parameter | Value |
|-----------|-------|
| Booking Fee | ₹25 |
| Per Km Rate | ₹15 |
| Minimum Fare | ₹100 |

### Surge Multipliers

| Demand Level | Active Bookings | Multiplier |
|--------------|-----------------|------------|
| Low | < 50 | 1.0x |
| Medium | 50 - 100 | 1.2x |
| High | 100 - 200 | 1.5x |
| Very High | > 200 | 2.0x |

### Sharing Discounts

| Factor | Discount |
|--------|----------|
| Per Co-passenger | 5% |
| Maximum Discount | 25% |

### Cab Type Multipliers

| Cab Type | Multiplier |
|----------|------------|
| SEDAN | 1.0x |
| SUV | 1.3x |
| VAN | 1.5x |
| PREMIUM_SEDAN | 1.8x |

### Cancellation Policy

| Timing | Fee |
|--------|-----|
| > 10 minutes before pickup | Free |
| ≤ 10 minutes before pickup | 20% of fare |

---

## ⚠ Error Handling

### Standard Error Response

```json
{
    "success": false,
    "message": "Error description",
    "errorCode": "ERROR_CODE",
    "timestamp": "2026-02-17T10:00:00"
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid request data |
| `RESOURCE_NOT_FOUND` | 404 | Resource not found |
| `DUPLICATE_RESOURCE` | 409 | Resource already exists |
| `NO_CAB_AVAILABLE` | 503 | No cabs available |
| `INVALID_STATUS_FOR_CANCELLATION` | 400 | Cannot cancel booking |
| `BOOKING_CONSTRAINT_VIOLATION` | 400 | Booking constraints not met |

---

## 📁 Project Structure

```
shared-cab-booking/
├── src/
│   ├── main/
│   │   ├── java/com/kucp1127/sharedcabbooking/
│   │   │   ├── config/           # Configuration classes
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── RedisConfig.java
│   │   │   ├── controller/       # REST Controllers
│   │   │   │   ├── BookingController.java
│   │   │   │   ├── CabController.java
│   │   │   │   ├── PassengerController.java
│   │   │   │   ├── PricingController.java
│   │   │   │   └── RideController.java
│   │   │   ├── domain/
│   │   │   │   ├── entity/       # JPA Entities
│   │   │   │   └── enums/        # Enumerations
│   │   │   ├── dto/
│   │   │   │   ├── request/      # Request DTOs
│   │   │   │   ├── response/     # Response DTOs
│   │   │   │   └── mapper/       # DTO Mappers
│   │   │   ├── event/            # Domain Events
│   │   │   ├── exception/        # Custom Exceptions
│   │   │   ├── repository/       # JPA Repositories
│   │   │   ├── service/
│   │   │   │   ├── booking/      # Booking Services
│   │   │   │   ├── locking/      # Distributed Locking
│   │   │   │   ├── matching/     # Ride Matching Strategies
│   │   │   │   └── pricing/      # Pricing Strategies
│   │   │   └── util/             # Utility Classes
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── redisson.yaml
│   │       └── db/migration/     # Flyway Migrations
│   └── test/                     # Test Classes
├── pom.xml
├── ARCHITECTURE.md
├── API_DOCUMENTATION.md
└── README.md
```

---

## 📄 License

This project is licensed under the MIT License.

---

## 👨‍💻 Author

**Adarsh Dubey**

---

## 🙏 Acknowledgments

- Spring Boot Team
- Redisson Project
- PostgreSQL Community
- Redis Community

