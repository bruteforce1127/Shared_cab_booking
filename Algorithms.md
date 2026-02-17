# 🧮 Algorithms & Data Structures

This document provides a comprehensive analysis of all algorithms and data structures used in the Smart Airport Ride Pooling System, including complexity analysis and design rationale.

---

## 📋 Table of Contents

- [Overview](#overview)
- [1. Distance Calculation - Haversine Formula](#1-distance-calculation---haversine-formula)
- [2. Ride Matching Algorithm](#2-ride-matching-algorithm)
- [3. Dynamic Pricing Algorithm](#3-dynamic-pricing-algorithm)
- [4. Cab Assignment Algorithm](#4-cab-assignment-algorithm)
- [5. Route Optimization](#5-route-optimization)
- [6. Distributed Locking](#6-distributed-locking)
- [7. Cancellation & Rebalancing](#7-cancellation--rebalancing)
- [8. Data Structures Used](#8-data-structures-used)
- [9. Complexity Summary](#9-complexity-summary)

---

## Overview

The system employs several algorithms optimized for real-time ride pooling:

| Algorithm | Purpose | Time Complexity | Space Complexity |
|-----------|---------|-----------------|------------------|
| Haversine Distance | Geo-distance calculation | O(1) | O(1) |
| Greedy Nearest Neighbor | Ride matching | O(n²) | O(n) |
| Chain of Responsibility | Dynamic pricing | O(k) | O(1) |
| Proximity Search | Cab finding | O(n) | O(m) |
| Red-Black Tree (Redis) | Distributed locking | O(log n) | O(n) |

---

## 1. Distance Calculation - Haversine Formula

### Problem Statement
Calculate the great-circle distance between two points on Earth given their latitude and longitude coordinates.

### Algorithm

```
Location: util/GeoUtils.java
```

#### Mathematical Formula

```
a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)
c = 2 × atan2(√a, √(1-a))
distance = R × c

Where:
- R = Earth's radius (6,371 km)
- lat1, lat2 = Latitude of points 1 and 2 (in radians)
- Δlat = lat2 - lat1
- Δlon = lon2 - lon1
```

#### Pseudocode

```
FUNCTION haversineDistance(lat1, lon1, lat2, lon2):
    R = 6371.0  // Earth's radius in kilometers
    
    // Convert to radians
    lat1_rad = toRadians(lat1)
    lat2_rad = toRadians(lat2)
    delta_lat = toRadians(lat2 - lat1)
    delta_lon = toRadians(lon2 - lon1)
    
    // Haversine formula
    a = sin(delta_lat/2)² + cos(lat1_rad) × cos(lat2_rad) × sin(delta_lon/2)²
    c = 2 × atan2(√a, √(1-a))
    
    RETURN R × c
END FUNCTION
```

#### Complexity Analysis

| Metric | Value | Explanation |
|--------|-------|-------------|
| **Time Complexity** | O(1) | Fixed number of mathematical operations |
| **Space Complexity** | O(1) | Only primitive variables used |
| **Accuracy** | ~0.5% error | Assumes spherical Earth |

#### Usage in System

```java
// Calculate distance between pickup points
double distance = GeoUtils.calculateDistance(
    pickup1.getLatitude(), pickup1.getLongitude(),
    pickup2.getLatitude(), pickup2.getLongitude()
);
```

---

## 2. Ride Matching Algorithm

### Problem Statement
Group passengers into shared rides based on:
- Proximity of pickup locations (within 5 km)
- Compatible time windows (within 15 minutes)
- Same destination (airport)
- Cab capacity constraints

### Algorithm: Greedy Nearest Neighbor with Constraints

```
Location: service/matching/GreedyNearestNeighborStrategy.java
```

#### Algorithm Design

```
┌─────────────────────────────────────────────────────────────────┐
│                    RIDE MATCHING PIPELINE                        │
├─────────────────────────────────────────────────────────────────┤
│  Step 1: Filter Compatible Bookings                             │
│    └─> Time window check (±15 min)                              │
│    └─> Destination check (same airport)                         │
│    └─> Status check (PENDING only)                              │
│                                                                  │
│  Step 2: Build Proximity Graph                                   │
│    └─> Calculate pairwise distances                             │
│    └─> Filter by max radius (5 km)                              │
│                                                                  │
│  Step 3: Greedy Clustering                                       │
│    └─> Sort by pickup time                                      │
│    └─> Greedily assign to nearest compatible group              │
│    └─> Respect capacity constraints                             │
│                                                                  │
│  Step 4: Assign Optimal Cab                                      │
│    └─> Find nearest available cab                               │
│    └─> Verify capacity (passengers + luggage)                   │
└─────────────────────────────────────────────────────────────────┘
```

#### Pseudocode

```
FUNCTION findMatchingRides(newBooking, existingBookings):
    candidates = []
    
    // Step 1: Filter by time window
    FOR EACH booking IN existingBookings:
        IF |booking.pickupTime - newBooking.pickupTime| <= 15 minutes:
            IF booking.destination == newBooking.destination:
                IF booking.status == PENDING:
                    candidates.ADD(booking)
    
    // Step 2: Calculate distances and filter by proximity
    validCandidates = []
    FOR EACH candidate IN candidates:
        distance = haversineDistance(
            newBooking.pickup, 
            candidate.pickup
        )
        IF distance <= MAX_PROXIMITY_RADIUS (5 km):
            validCandidates.ADD({candidate, distance})
    
    // Step 3: Sort by distance (nearest first)
    SORT validCandidates BY distance ASC
    
    // Step 4: Find compatible group with capacity
    FOR EACH candidate IN validCandidates:
        group = candidate.rideGroup
        IF group.canAccommodate(newBooking):
            IF checkDetourTolerance(group, newBooking):
                RETURN group
    
    // No match found - create new group
    RETURN createNewRideGroup(newBooking)
END FUNCTION
```

#### Constraint Validation

```
FUNCTION canAccommodate(group, newBooking):
    cab = group.assignedCab
    
    // Check passenger capacity
    totalPassengers = group.currentPassengers + newBooking.passengerCount
    IF totalPassengers > cab.maxPassengers:
        RETURN false
    
    // Check luggage capacity
    totalLuggage = group.currentLuggage + newBooking.luggageCount
    IF totalLuggage > cab.maxLuggage:
        RETURN false
    
    // Check weight capacity
    totalWeight = group.currentWeight + newBooking.luggageWeight
    IF totalWeight > cab.maxWeight:
        RETURN false
    
    RETURN true
END FUNCTION
```

#### Detour Tolerance Check

```
FUNCTION checkDetourTolerance(group, newBooking):
    // Calculate original route distance
    originalDistance = calculateGroupRouteDistance(group)
    
    // Calculate new route with additional pickup
    newDistance = calculateRouteWithNewPickup(group, newBooking)
    
    // Calculate detour percentage
    detourPercent = (newDistance - originalDistance) / originalDistance
    
    // Check against all passengers' tolerance
    FOR EACH booking IN group.bookings:
        IF detourPercent > booking.maxDetourTolerance:
            RETURN false
    
    IF detourPercent > newBooking.maxDetourTolerance:
        RETURN false
    
    RETURN true
END FUNCTION
```

#### Complexity Analysis

| Metric | Value | Explanation |
|--------|-------|-------------|
| **Time Complexity** | O(n²) | Pairwise distance calculation for n bookings |
| **Space Complexity** | O(n) | Store candidates and distances |
| **Best Case** | O(n) | First candidate matches |
| **Worst Case** | O(n²) | No matches, check all pairs |

#### Optimization Strategies

1. **Spatial Indexing**: Use R-tree or Quadtree for O(log n) proximity queries
2. **Time-based Partitioning**: Bucket bookings by time slots
3. **Caching**: Cache frequently accessed distance calculations

---

## 3. Dynamic Pricing Algorithm

### Problem Statement
Calculate ride fare dynamically based on:
- Base fare + distance-based charges
- Surge pricing (demand-based)
- Cab type multipliers
- Sharing discounts

### Algorithm: Chain of Responsibility Pattern

```
Location: service/pricing/
```

#### Pricing Chain Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRICING CHAIN                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │  Base Fare   │───>│    Surge     │───>│   Cab Type       │   │
│  │  Calculator  │    │  Multiplier  │    │   Multiplier     │   │
│  └──────────────┘    └──────────────┘    └──────────────────┘   │
│         │                   │                     │              │
│         ▼                   ▼                     ▼              │
│    BaseFare +          × Surge            × CabType              │
│    (Distance × Rate)   Factor             Factor                 │
│                                                                  │
│                    ┌──────────────────┐                          │
│                    │  Sharing         │                          │
│               ───>│  Discount        │───> Final Price          │
│                    └──────────────────┘                          │
│                           │                                      │
│                           ▼                                      │
│                    × (1 - Discount%)                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Master Pricing Formula

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                  │
│  FinalPrice = max(MinimumFare, CalculatedFare)                  │
│                                                                  │
│  Where:                                                          │
│  CalculatedFare = BaseFare × SurgeMultiplier × CabMultiplier    │
│                   × (1 - SharingDiscount)                        │
│                                                                  │
│  BaseFare = BookingFee + (Distance × PerKmRate)                 │
│                                                                  │
│  SharingDiscount = min(MaxDiscount, CoPassengers × DiscountRate)│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Step 1: Base Fare Calculation

```
FUNCTION calculateBaseFare(booking):
    distance = haversineDistance(
        booking.pickup,
        booking.dropoff
    )
    
    baseFare = BOOKING_FEE + (distance × PER_KM_RATE)
    
    RETURN {
        fare: baseFare,
        distance: distance,
        components: {
            bookingFee: BOOKING_FEE,
            distanceCharge: distance × PER_KM_RATE
        }
    }
END FUNCTION

Constants:
- BOOKING_FEE = ₹25
- PER_KM_RATE = ₹15
- MINIMUM_FARE = ₹100
```

#### Step 2: Surge Pricing Calculation

```
FUNCTION calculateSurgeMultiplier():
    activeBookings = countActiveBookings()  // O(1) with caching
    
    IF activeBookings < 50:
        RETURN 1.0      // No surge
    ELSE IF activeBookings < 100:
        RETURN 1.2      // Low surge
    ELSE IF activeBookings < 200:
        RETURN 1.5      // Medium surge
    ELSE:
        RETURN 2.0      // High surge
END FUNCTION
```

**Surge Multiplier Visualization:**

```
Multiplier
    ▲
2.0 │                         ┌────────────
    │                         │
1.5 │              ┌──────────┘
    │              │
1.2 │    ┌────────┘
    │    │
1.0 │────┘
    └────┴─────────┴──────────┴───────────▶ Active Bookings
         50       100        200
```

#### Step 3: Cab Type Multiplier

```
FUNCTION getCabTypeMultiplier(cabType):
    SWITCH cabType:
        CASE SEDAN:         RETURN 1.0
        CASE SUV:           RETURN 1.3
        CASE VAN:           RETURN 1.5
        CASE PREMIUM_SEDAN: RETURN 1.8
        DEFAULT:            RETURN 1.0
END FUNCTION
```

#### Step 4: Sharing Discount

```
FUNCTION calculateSharingDiscount(rideGroup):
    coPassengers = rideGroup.totalBookings - 1  // Exclude self
    
    IF coPassengers <= 0:
        RETURN 0.0
    
    discount = coPassengers × DISCOUNT_PER_PASSENGER
    
    RETURN min(discount, MAX_DISCOUNT)
END FUNCTION

Constants:
- DISCOUNT_PER_PASSENGER = 0.05 (5%)
- MAX_DISCOUNT = 0.25 (25%)
```

**Discount Table:**

| Co-Passengers | Discount |
|---------------|----------|
| 0 | 0% |
| 1 | 5% |
| 2 | 10% |
| 3 | 15% |
| 4 | 20% |
| 5+ | 25% (max) |

#### Complete Pricing Algorithm

```
FUNCTION calculateFinalPrice(booking, rideGroup):
    // Step 1: Base fare
    baseResult = calculateBaseFare(booking)
    
    // Step 2: Surge multiplier
    surgeMultiplier = calculateSurgeMultiplier()
    
    // Step 3: Cab type multiplier
    cabMultiplier = getCabTypeMultiplier(rideGroup.cab.type)
    
    // Step 4: Sharing discount
    sharingDiscount = calculateSharingDiscount(rideGroup)
    
    // Calculate final price
    calculatedFare = baseResult.fare 
                     × surgeMultiplier 
                     × cabMultiplier 
                     × (1 - sharingDiscount)
    
    finalPrice = max(MINIMUM_FARE, calculatedFare)
    
    RETURN {
        finalPrice: finalPrice,
        breakdown: {
            baseFare: baseResult.fare,
            distance: baseResult.distance,
            surgeMultiplier: surgeMultiplier,
            cabMultiplier: cabMultiplier,
            sharingDiscount: sharingDiscount,
            minimumFareApplied: finalPrice == MINIMUM_FARE
        }
    }
END FUNCTION
```

#### Pricing Example

```
Scenario: Airport ride with 2 co-passengers during peak hours

Input:
- Distance: 25 km
- Cab Type: SUV
- Active Bookings: 150 (surge)
- Co-passengers: 2

Calculation:
┌────────────────────────────────────────────────────────────┐
│ Base Fare    = ₹25 + (25 × ₹15) = ₹400                     │
│ Surge        = 1.5× (150 active bookings)                  │
│ Cab Type     = 1.3× (SUV)                                  │
│ Discount     = 10% (2 co-passengers)                       │
│                                                             │
│ Final Price  = ₹400 × 1.5 × 1.3 × 0.90                     │
│              = ₹702                                         │
└────────────────────────────────────────────────────────────┘
```

#### Complexity Analysis

| Metric | Value | Explanation |
|--------|-------|-------------|
| **Time Complexity** | O(k) | k = number of pricing strategies in chain |
| **Space Complexity** | O(1) | Fixed number of variables |
| **Chain Length** | 4 | Base → Surge → CabType → Discount |

---

## 4. Cab Assignment Algorithm

### Problem Statement
Find the optimal available cab for a ride group based on:
- Proximity to first pickup point
- Cab type compatibility
- Capacity requirements

### Algorithm: Priority-based Selection

```
Location: service/CabService.java
```

#### Pseudocode

```
FUNCTION findOptimalCab(rideGroup):
    requiredType = rideGroup.preferredCabType
    firstPickup = rideGroup.getFirstPickupLocation()
    
    // Query available cabs
    availableCabs = cabRepository.findByStatusAndType(
        AVAILABLE, 
        requiredType
    )
    
    IF availableCabs.isEmpty():
        // Fallback: Try larger cab types
        availableCabs = findUpgradeCabs(requiredType)
    
    IF availableCabs.isEmpty():
        THROW NoCabAvailableException
    
    // Calculate distances and sort
    cabDistances = []
    FOR EACH cab IN availableCabs:
        distance = haversineDistance(
            cab.currentLocation,
            firstPickup
        )
        cabDistances.ADD({cab, distance})
    
    // Sort by distance (nearest first)
    SORT cabDistances BY distance ASC
    
    // Return nearest cab that meets capacity
    FOR EACH {cab, distance} IN cabDistances:
        IF cab.canAccommodate(rideGroup):
            RETURN cab
    
    THROW NoCabAvailableException
END FUNCTION
```

#### Cab Upgrade Logic

```
FUNCTION findUpgradeCabs(requestedType):
    upgradeOrder = getUpgradeOrder(requestedType)
    
    FOR EACH type IN upgradeOrder:
        cabs = cabRepository.findByStatusAndType(AVAILABLE, type)
        IF NOT cabs.isEmpty():
            RETURN cabs
    
    RETURN []
END FUNCTION

FUNCTION getUpgradeOrder(type):
    SWITCH type:
        CASE SEDAN:         RETURN [SUV, VAN]
        CASE SUV:           RETURN [VAN]
        CASE PREMIUM_SEDAN: RETURN [SUV, VAN]
        CASE VAN:           RETURN []
END FUNCTION
```

#### Complexity Analysis

| Metric | Value | Explanation |
|--------|-------|-------------|
| **Time Complexity** | O(n log n) | Sort n available cabs by distance |
| **Space Complexity** | O(n) | Store cab-distance pairs |
| **Database Query** | O(n) | Filter available cabs |

---

## 5. Route Optimization

### Problem Statement
Optimize the pickup sequence for multiple passengers to minimize total travel distance.

### Algorithm: Greedy Insertion with Detour Constraints

```
Location: service/matching/RouteOptimizer.java
```

#### Approach

```
FUNCTION optimizePickupRoute(rideGroup):
    pickups = rideGroup.getAllPickupLocations()
    destination = rideGroup.destination  // Airport
    
    // Start with the first booking's pickup
    route = [pickups[0]]
    remaining = pickups[1:]
    
    // Greedy insertion
    WHILE remaining NOT empty:
        bestPickup = null
        bestPosition = -1
        minAddedDistance = INFINITY
        
        FOR EACH pickup IN remaining:
            FOR position = 0 TO route.length:
                addedDistance = calculateInsertionCost(
                    route, 
                    pickup, 
                    position
                )
                
                IF addedDistance < minAddedDistance:
                    IF checkAllDetourTolerances(route, pickup, position):
                        minAddedDistance = addedDistance
                        bestPickup = pickup
                        bestPosition = position
        
        IF bestPickup != null:
            route.INSERT(bestPosition, bestPickup)
            remaining.REMOVE(bestPickup)
        ELSE:
            // Cannot add more pickups within constraints
            BREAK
    
    // Add destination at the end
    route.ADD(destination)
    
    RETURN route
END FUNCTION
```

#### Insertion Cost Calculation

```
FUNCTION calculateInsertionCost(route, newPickup, position):
    IF position == 0:
        // Insert at beginning
        RETURN distance(newPickup, route[0])
    ELSE IF position == route.length:
        // Insert at end
        RETURN distance(route[last], newPickup)
    ELSE:
        // Insert in middle
        before = route[position - 1]
        after = route[position]
        
        oldDistance = distance(before, after)
        newDistance = distance(before, newPickup) + distance(newPickup, after)
        
        RETURN newDistance - oldDistance
END FUNCTION
```

#### Complexity Analysis

| Metric | Value | Explanation |
|--------|-------|-------------|
| **Time Complexity** | O(n³) | n pickups × n positions × n tolerance checks |
| **Space Complexity** | O(n) | Store route and remaining pickups |
| **Approximation Ratio** | ~1.5 | Greedy provides 1.5× optimal for TSP |

---

## 6. Distributed Locking

### Problem Statement
Prevent race conditions when multiple requests try to book the same cab or modify the same ride group concurrently.

### Algorithm: Redis-based Distributed Lock (Redlock)

```
Location: service/locking/DistributedLockService.java
```

#### Lock Acquisition

```
FUNCTION acquireLock(resourceId, timeout):
    lockKey = "lock:" + resourceId
    lockValue = generateUniqueId()  // UUID
    
    // Try to acquire lock with NX (Not eXists) and PX (expiry in ms)
    acquired = redis.SET(lockKey, lockValue, NX, PX, timeout)
    
    IF acquired:
        RETURN {success: true, lockValue: lockValue}
    ELSE:
        RETURN {success: false}
END FUNCTION
```

#### Lock Release

```
FUNCTION releaseLock(resourceId, lockValue):
    lockKey = "lock:" + resourceId
    
    // Lua script for atomic check-and-delete
    script = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
    """
    
    result = redis.EVAL(script, [lockKey], [lockValue])
    RETURN result == 1
END FUNCTION
```

#### Usage Pattern

```
FUNCTION bookRideWithLock(rideGroupId, booking):
    lock = acquireLock("rideGroup:" + rideGroupId, 10000)  // 10 sec
    
    IF NOT lock.success:
        THROW ConcurrentModificationException
    
    TRY:
        // Critical section
        rideGroup = repository.findById(rideGroupId)
        
        IF rideGroup.canAccommodate(booking):
            rideGroup.addBooking(booking)
            repository.save(rideGroup)
            RETURN success
        ELSE:
            RETURN failure
    FINALLY:
        releaseLock("rideGroup:" + rideGroupId, lock.lockValue)
END FUNCTION
```

#### Complexity Analysis

| Metric | Value | Explanation |
|--------|-------|-------------|
| **Time Complexity** | O(1) | Redis SET/GET operations |
| **Space Complexity** | O(1) | Single key-value pair |
| **Lock Timeout** | 10 seconds | TTL for automatic release |
| **Contention Handling** | Retry with backoff | Exponential backoff strategy |

---

## 7. Cancellation & Rebalancing

### Problem Statement
Handle booking cancellations and automatically rebalance ride groups to maintain optimal assignments.

### Algorithm: Event-Driven Rebalancing

```
Location: service/booking/CancellationService.java
Location: service/booking/RideGroupRebalancer.java
```

#### Cancellation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                  CANCELLATION PIPELINE                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Validate Cancellation                                        │
│     └─> Check booking status (must be PENDING/CONFIRMED)        │
│     └─> Calculate cancellation fee                              │
│                                                                  │
│  2. Process Cancellation                                         │
│     └─> Update booking status to CANCELLED                      │
│     └─> Remove from ride group                                  │
│     └─> Calculate refund amount                                 │
│                                                                  │
│  3. Publish Event (Async)                                        │
│     └─> BookingCancelledEvent                                   │
│                                                                  │
│  4. Rebalance Ride Group (Event Listener)                       │
│     └─> Check if group is still viable                          │
│     └─> Reassign cab if needed                                  │
│     └─> Recalculate prices for remaining passengers             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Cancellation Fee Calculation

```
FUNCTION calculateCancellationFee(booking):
    pickupTime = booking.requestedPickupTime
    currentTime = now()
    
    minutesUntilPickup = (pickupTime - currentTime) / MINUTES
    
    IF minutesUntilPickup > 10:
        RETURN 0  // Free cancellation
    ELSE:
        RETURN booking.fare × 0.20  // 20% fee
END FUNCTION
```

#### Rebalancing Algorithm

```
FUNCTION rebalanceRideGroup(rideGroup, cancelledBooking):
    // Remove cancelled booking
    rideGroup.bookings.REMOVE(cancelledBooking)
    
    IF rideGroup.bookings.isEmpty():
        // Dissolve group and free cab
        rideGroup.status = DISSOLVED
        rideGroup.cab.status = AVAILABLE
        RETURN
    
    // Check if current cab is still optimal
    IF NOT isOptimalCab(rideGroup):
        // Try to find better cab
        newCab = findOptimalCab(rideGroup)
        IF newCab != null:
            rideGroup.cab.status = AVAILABLE
            rideGroup.cab = newCab
            newCab.status = ASSIGNED
    
    // Recalculate prices for remaining passengers
    FOR EACH booking IN rideGroup.bookings:
        newPrice = calculateFinalPrice(booking, rideGroup)
        booking.fare = newPrice
        // Notify passenger of price update
        notifyPriceChange(booking, newPrice)
    
    repository.save(rideGroup)
END FUNCTION
```

#### Complexity Analysis

| Metric | Value | Explanation |
|--------|-------|-------------|
| **Time Complexity** | O(n + m) | n = bookings in group, m = available cabs |
| **Space Complexity** | O(1) | In-place modifications |
| **Event Processing** | Async | Non-blocking cancellation response |

---

## 8. Data Structures Used

### 8.1 Priority Queue (Min-Heap)

**Purpose:** Find nearest cabs efficiently

```
Usage: Cab selection by distance
Operations:
  - Insert: O(log n)
  - Extract-Min: O(log n)
  - Peek: O(1)
```

### 8.2 HashMap

**Purpose:** Fast lookups for bookings, passengers, cabs

```
Usage: In-memory caching, entity lookups
Operations:
  - Get: O(1) average
  - Put: O(1) average
  - Contains: O(1) average
```

### 8.3 ArrayList

**Purpose:** Store bookings in ride groups

```
Usage: Maintaining ordered lists
Operations:
  - Add: O(1) amortized
  - Get by index: O(1)
  - Remove: O(n)
```

### 8.4 TreeMap (Red-Black Tree)

**Purpose:** Sorted storage for time-based queries

```
Usage: Finding bookings within time windows
Operations:
  - Insert: O(log n)
  - Search: O(log n)
  - Range query: O(log n + k)
```

### 8.5 Redis Sorted Set

**Purpose:** Maintain cab locations for proximity queries

```
Usage: Geo-spatial indexing (potential enhancement)
Operations:
  - ZADD: O(log n)
  - ZRANGEBYSCORE: O(log n + m)
  - GEORADIUS: O(n) without geo-index
```

---

## 9. Complexity Summary

### Overall System Complexity

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| **Book Ride** | O(n²) | O(n) | Dominated by matching |
| **Get Fare Estimate** | O(1) | O(1) | Chain calculation |
| **Cancel Booking** | O(n + m) | O(1) | Rebalancing included |
| **Find Available Cabs** | O(n log n) | O(n) | Sort by distance |
| **Update Cab Location** | O(1) | O(1) | Simple update |
| **Get Ride Status** | O(1) | O(1) | Direct lookup |

### Database Query Complexity

| Query | Complexity | Index Used |
|-------|------------|------------|
| Find booking by ID | O(1) | Primary key |
| Find cabs by status | O(n) | Status index |
| Find bookings by time range | O(log n + k) | Time index |
| Find passengers by email | O(1) | Unique index |
| Find nearby cabs | O(n) | Full scan (no geo-index) |

### Optimization Opportunities

| Current | Optimized | Improvement |
|---------|-----------|-------------|
| O(n²) matching | O(n log n) with R-tree | Spatial indexing |
| O(n) cab proximity | O(log n) with geo-index | Redis GEOSEARCH |
| O(n) time filtering | O(log n) with B-tree | Time partitioning |

---

## 📚 References

1. **Haversine Formula**: Sinnott, R.W. (1984). "Virtues of the Haversine"
2. **Greedy Algorithms**: Cormen et al. "Introduction to Algorithms"
3. **Distributed Locks**: Martin Kleppmann. "Designing Data-Intensive Applications"
4. **Chain of Responsibility**: Gang of Four. "Design Patterns"


