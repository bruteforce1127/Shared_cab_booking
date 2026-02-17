# Smart Airport Ride Pooling API Documentation

## Base URL
```
http://localhost:8080
```

## Swagger UI
Access the interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

---

## API Call Order (Step-by-Step Testing Guide)

Follow this order to test the complete ride pooling flow:

### Step 1: Register Passengers
### Step 2: Register Cabs
### Step 3: Get Fare Estimate
### Step 4: Book a Ride
### Step 5: View Ride Details
### Step 6: (Optional) Cancel Booking

---

## 1. Passenger Management APIs

### 1.1 Register a New Passenger
**This should be called FIRST to create passengers in the system.**

```http
POST /api/v1/passengers
Content-Type: application/json
```

**Request Body:**
```json
{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+919876543210",
    "detourTolerance": 0.20,
    "preferredCabType": "SEDAN"
}
```

**Response (201 Created):**
```json
{
    "success": true,
    "message": "Passenger registered successfully",
    "data": {
        "id": 1,
        "name": "John Doe",
        "email": "john.doe@example.com",
        "phone": "+919876543210",
        "detourTolerance": 0.20,
        "preferredCabType": "SEDAN",
        "rating": 5.0,
        "totalRides": 0
    },
    "timestamp": "2026-02-15T10:30:00"
}
```

### 1.2 Get Passenger by ID
```http
GET /api/v1/passengers/{id}
```

**Example:** `GET /api/v1/passengers/1`

### 1.3 Get Passenger by Email
```http
GET /api/v1/passengers/email/{email}
```

**Example:** `GET /api/v1/passengers/email/john.doe@example.com`

### 1.4 Get All Passengers
```http
GET /api/v1/passengers
```

### 1.5 Update Passenger
```http
PUT /api/v1/passengers/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
    "name": "John Doe Updated",
    "email": "john.doe@example.com",
    "phone": "+919876543211",
    "detourTolerance": 0.25,
    "preferredCabType": "SUV"
}
```

### 1.6 Delete Passenger
```http
DELETE /api/v1/passengers/{id}
```

---

## 2. Cab Management APIs

### 2.1 Register a New Cab
**This should be called SECOND to add cabs to the system.**

```http
POST /api/v1/cabs
Content-Type: application/json
```

**Request Body:**
```json
{
    "licensePlate": "DL-01-AB-1234",
    "driverName": "Rajesh Kumar",
    "driverPhone": "+919876543220",
    "cabType": "SEDAN",
    "currentLatitude": 28.6139,
    "currentLongitude": 77.2090,
    "currentAddress": "Connaught Place, New Delhi"
}
```

**Cab Types Available:**
- `SEDAN` - 4 passengers, 3 luggage, 100kg max
- `SUV` - 6 passengers, 5 luggage, 150kg max
- `VAN` - 8 passengers, 8 luggage, 200kg max
- `PREMIUM_SEDAN` - 4 passengers, 3 luggage, 100kg max (premium pricing)

**Response (201 Created):**
```json
{
    "success": true,
    "message": "Cab registered successfully",
    "data": {
        "id": 1,
        "licensePlate": "DL-01-AB-1234",
        "driverName": "Rajesh Kumar",
        "driverPhone": "+919876543220",
        "cabType": "SEDAN",
        "status": "AVAILABLE",
        "currentLatitude": 28.6139,
        "currentLongitude": 77.2090,
        "currentAddress": "Connaught Place, New Delhi",
        "driverRating": 5.0,
        "availableSeats": 4,
        "availableLuggageCapacityKg": 100.0
    },
    "timestamp": "2026-02-15T10:35:00"
}
```

### 2.2 Get Cab by ID
```http
GET /api/v1/cabs/{id}
```

### 2.3 Get Cab by License Plate
```http
GET /api/v1/cabs/license/{licensePlate}
```

**Example:** `GET /api/v1/cabs/license/DL-01-AB-1234`

### 2.4 Get All Cabs
```http
GET /api/v1/cabs
```

### 2.5 Get Available Cabs
```http
GET /api/v1/cabs/available
```

### 2.6 Get Available Cabs by Type
```http
GET /api/v1/cabs/available/type/{cabType}
```

**Example:** `GET /api/v1/cabs/available/type/SEDAN`

### 2.7 Get Nearby Cabs
```http
GET /api/v1/cabs/nearby?latitude={lat}&longitude={lng}&radiusKm={radius}
```

**Example:** `GET /api/v1/cabs/nearby?latitude=28.6139&longitude=77.2090&radiusKm=5.0`

### 2.8 Update Cab Location
```http
PATCH /api/v1/cabs/{id}/location?latitude={lat}&longitude={lng}&address={address}
```

**Example:** `PATCH /api/v1/cabs/1/location?latitude=28.5500&longitude=77.2500&address=Nehru Place`

### 2.9 Update Cab Status
```http
PATCH /api/v1/cabs/{id}/status?status={status}
```

**Cab Statuses:**
- `AVAILABLE` - Ready for new rides
- `ASSIGNED` - Assigned to a ride group
- `EN_ROUTE` - En route to pickup
- `ON_TRIP` - Currently on a trip
- `OFFLINE` - Not available

**Example:** `PATCH /api/v1/cabs/1/status?status=AVAILABLE`

### 2.10 Delete Cab
```http
DELETE /api/v1/cabs/{id}
```

---

## 3. Pricing APIs

### 3.1 Get Fare Estimate
**Call this THIRD to get estimated fare before booking.**

```http
POST /api/v1/pricing/estimate
Content-Type: application/json
```

**Request Body:**
```json
{
    "passengerId": 1,
    "pickupLatitude": 28.6139,
    "pickupLongitude": 77.2090,
    "pickupAddress": "Connaught Place, New Delhi",
    "dropoffLatitude": 28.5562,
    "dropoffLongitude": 77.1000,
    "dropoffAddress": "Indira Gandhi International Airport",
    "requestedPickupTime": "2026-02-15T14:00:00",
    "passengerCount": 1,
    "luggageWeightKg": 15.0,
    "luggageCount": 2,
    "maxDetourTolerance": 0.20,
    "preferredCabType": "SEDAN"
}
```

**Response:**
```json
{
    "success": true,
    "message": "Fare estimate calculated",
    "data": {
        "baseFare": 225.00,
        "distanceCharge": 200.00,
        "bookingFee": 25.00,
        "surgeCharge": 0.00,
        "estimatedSharingDiscount": 22.50,
        "estimatedTotalFare": 202.50,
        "surgeMultiplier": 1.0,
        "estimatedDistanceKm": 13.33,
        "estimatedCoPassengers": 2,
        "message": "Share your ride and save up to 10%!"
    },
    "timestamp": "2026-02-15T10:40:00"
}
```

### 3.2 Get Current Surge Multiplier
```http
GET /api/v1/pricing/surge
```

**Response:**
```json
{
    "success": true,
    "data": {
        "surgeMultiplier": 1.2,
        "surgePercentage": 20.0,
        "surgeActive": true
    },
    "timestamp": "2026-02-15T10:41:00"
}
```

---

## 4. Ride Booking APIs

### 4.1 Book a Ride
**Call this FOURTH to create a ride booking.**

```http
POST /api/v1/rides
Content-Type: application/json
```

**Request Body:**
```json
{
    "passengerId": 1,
    "pickupLatitude": 28.6139,
    "pickupLongitude": 77.2090,
    "pickupAddress": "Connaught Place, New Delhi",
    "dropoffLatitude": 28.5562,
    "dropoffLongitude": 77.1000,
    "dropoffAddress": "Indira Gandhi International Airport",
    "requestedPickupTime": "2026-02-15T14:00:00",
    "passengerCount": 1,
    "luggageWeightKg": 15.0,
    "luggageCount": 2,
    "maxDetourTolerance": 0.20,
    "preferredCabType": "SEDAN",
    "specialRequirements": "Need help with luggage"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| passengerId | Long | Yes | ID of registered passenger |
| pickupLatitude | Double | Yes | Pickup location latitude (-90 to 90) |
| pickupLongitude | Double | Yes | Pickup location longitude (-180 to 180) |
| pickupAddress | String | No | Pickup address description |
| dropoffLatitude | Double | Yes | Dropoff location latitude |
| dropoffLongitude | Double | Yes | Dropoff location longitude |
| dropoffAddress | String | No | Dropoff address description |
| requestedPickupTime | DateTime | Yes | Requested pickup time (must be in future) |
| passengerCount | Integer | No | Number of passengers (1-6, default: 1) |
| luggageWeightKg | Double | No | Total luggage weight (0-200 kg, default: 0) |
| luggageCount | Integer | No | Number of luggage pieces (0-10, default: 0) |
| maxDetourTolerance | Double | No | Max acceptable detour (0.0-0.5, default: 0.20) |
| preferredCabType | String | No | SEDAN, SUV, VAN, or PREMIUM_SEDAN |
| specialRequirements | String | No | Any special requirements |

**Response (201 Created):**
```json
{
    "success": true,
    "message": "Ride booked successfully",
    "data": {
        "bookingId": 1,
        "rideGroupId": 1,
        "passengerId": 1,
        "status": "CONFIRMED",
        "pickupLocation": {
            "latitude": 28.6139,
            "longitude": 77.2090,
            "address": "Connaught Place, New Delhi"
        },
        "dropoffLocation": {
            "latitude": 28.5562,
            "longitude": 77.1000,
            "address": "Indira Gandhi International Airport"
        },
        "requestedPickupTime": "2026-02-15T14:00:00",
        "estimatedPickupTime": "2026-02-15T14:05:00",
        "estimatedArrivalTime": "2026-02-15T14:45:00",
        "passengerCount": 1,
        "luggageWeightKg": 15.0,
        "luggageCount": 2,
        "pickupSequence": 1,
        "estimatedFare": 202.50,
        "sharingDiscount": 22.50,
        "surgeMultiplier": 1.0,
        "totalCoPassengers": 0,
        "estimatedDetourPercentage": 0.0,
        "cabInfo": {
            "licensePlate": "DL-01-AB-1234",
            "driverName": "Rajesh Kumar",
            "driverPhone": "+919876543220",
            "cabType": "SEDAN",
            "driverRating": 5.0
        },
        "createdAt": "2026-02-15T10:45:00"
    },
    "timestamp": "2026-02-15T10:45:00"
}
```

### 4.2 Get Ride Details
```http
GET /api/v1/rides/{bookingId}
```

**Example:** `GET /api/v1/rides/1`

### 4.3 Get Passenger's Rides
```http
GET /api/v1/rides/passenger/{passengerId}
```

**Example:** `GET /api/v1/rides/passenger/1`

### 4.4 Get Passenger's Rides (Paginated)
```http
GET /api/v1/rides/passenger/{passengerId}/paginated?page=0&size=10&sort=createdAt,desc
```

### 4.5 Get Active Rides
```http
GET /api/v1/rides/passenger/{passengerId}/active
```

### 4.6 Get Ride Group Details
```http
GET /api/v1/rides/groups/{groupId}
```

**Response:**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "status": "FORMING",
        "cabInfo": {
            "licensePlate": "DL-01-AB-1234",
            "driverName": "Rajesh Kumar",
            "driverPhone": "+919876543220",
            "cabType": "SEDAN",
            "driverRating": 5.0
        },
        "airportLocation": {
            "latitude": 28.5562,
            "longitude": 77.1000,
            "address": "Indira Gandhi International Airport"
        },
        "scheduledDepartureTime": "2026-02-15T14:00:00",
        "estimatedArrivalTime": "2026-02-15T14:45:00",
        "totalPassengers": 2,
        "totalLuggageWeightKg": 25.0,
        "totalDistanceKm": 18.5,
        "detourPercentage": 0.12,
        "bookings": [
            {
                "bookingId": 1,
                "passengerId": 1,
                "passengerName": "John Doe",
                "pickupSequence": 1,
                "pickupLocation": {
                    "latitude": 28.6139,
                    "longitude": 77.2090,
                    "address": "Connaught Place"
                },
                "estimatedPickupTime": "2026-02-15T14:05:00"
            },
            {
                "bookingId": 2,
                "passengerId": 2,
                "passengerName": "Jane Smith",
                "pickupSequence": 2,
                "pickupLocation": {
                    "latitude": 28.6000,
                    "longitude": 77.2200,
                    "address": "India Gate"
                },
                "estimatedPickupTime": "2026-02-15T14:12:00"
            }
        ]
    },
    "timestamp": "2026-02-15T10:50:00"
}
```

### 4.7 Get Ride Group for a Booking
```http
GET /api/v1/rides/{bookingId}/group
```

---

## 5. Booking Management APIs

### 5.1 Check if Booking Can Be Cancelled
```http
GET /api/v1/bookings/{bookingId}/can-cancel
```

**Response:**
```json
{
    "success": true,
    "data": {
        "canCancel": true
    },
    "timestamp": "2026-02-15T10:55:00"
}
```

### 5.2 Cancel a Booking
```http
POST /api/v1/bookings/cancel
Content-Type: application/json
```

**Request Body:**
```json
{
    "bookingId": 1,
    "reason": "Change of plans",
    "initiatedBy": "PASSENGER"
}
```

**Response:**
```json
{
    "success": true,
    "message": "Booking cancelled successfully",
    "data": {
        "cancellationId": 1,
        "bookingId": 1,
        "cancellationFee": 0.00,
        "refundAmount": 202.50,
        "cancelledAt": "2026-02-15T10:56:00"
    },
    "timestamp": "2026-02-15T10:56:00"
}
```

**Note:** Cancellation is free if done more than 10 minutes before pickup. After that, a 20% fee applies.

---

## Complete Testing Flow (cURL Examples)

### Step 1: Register a Passenger
```bash
curl -X POST http://localhost:8080/api/v1/passengers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+919876543210",
    "detourTolerance": 0.20
  }'
```

### Step 2: Register a Cab
```bash
curl -X POST http://localhost:8080/api/v1/cabs \
  -H "Content-Type: application/json" \
  -d '{
    "licensePlate": "DL-01-AB-1234",
    "driverName": "Rajesh Kumar",
    "driverPhone": "+919876543220",
    "cabType": "SEDAN",
    "currentLatitude": 28.6139,
    "currentLongitude": 77.2090
  }'
```

### Step 3: Get Fare Estimate
```bash
curl -X POST http://localhost:8080/api/v1/pricing/estimate \
  -H "Content-Type: application/json" \
  -d '{
    "passengerId": 1,
    "pickupLatitude": 28.6139,
    "pickupLongitude": 77.2090,
    "dropoffLatitude": 28.5562,
    "dropoffLongitude": 77.1000,
    "requestedPickupTime": "2026-02-15T14:00:00",
    "passengerCount": 1,
    "luggageWeightKg": 15.0
  }'
```

### Step 4: Book a Ride
```bash
curl -X POST http://localhost:8080/api/v1/rides \
  -H "Content-Type: application/json" \
  -d '{
    "passengerId": 1,
    "pickupLatitude": 28.6139,
    "pickupLongitude": 77.2090,
    "pickupAddress": "Connaught Place, New Delhi",
    "dropoffLatitude": 28.5562,
    "dropoffLongitude": 77.1000,
    "dropoffAddress": "IGI Airport",
    "requestedPickupTime": "2026-02-15T14:00:00",
    "passengerCount": 1,
    "luggageWeightKg": 15.0,
    "luggageCount": 2
  }'
```

### Step 5: Check Ride Status
```bash
curl http://localhost:8080/api/v1/rides/1
```

### Step 6: Cancel Booking (Optional)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 1,
    "reason": "Change of plans",
    "initiatedBy": "PASSENGER"
  }'
```

---

## Error Responses

### Validation Error (400)
```json
{
    "success": false,
    "message": "Validation failed",
    "errorCode": "VALIDATION_ERROR",
    "data": {
        "email": "Invalid email format",
        "phone": "Invalid phone number"
    },
    "timestamp": "2026-02-15T10:00:00"
}
```

### Resource Not Found (404)
```json
{
    "success": false,
    "message": "Passenger not found with id: 999",
    "errorCode": "RESOURCE_NOT_FOUND",
    "timestamp": "2026-02-15T10:00:00"
}
```

### No Cab Available (503)
```json
{
    "success": false,
    "message": "No cabs available at the moment",
    "errorCode": "NO_CAB_AVAILABLE",
    "timestamp": "2026-02-15T10:00:00"
}
```

### Cancellation Error (400)
```json
{
    "success": false,
    "message": "Cannot cancel booking with status: COMPLETED",
    "errorCode": "INVALID_STATUS_FOR_CANCELLATION",
    "timestamp": "2026-02-15T10:00:00"
}
```

---

## Booking Status Flow

```
PENDING → CONFIRMED → IN_PROGRESS → COMPLETED
    ↓          ↓
 EXPIRED   CANCELLED
```

## Ride Group Status Flow

```
FORMING → LOCKED → DISPATCHED → IN_PROGRESS → COMPLETED
    ↓
CANCELLED (if all bookings cancelled)
```

---

## Sample Locations (Delhi NCR)

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

## Pricing Formula

```
FinalPrice = (BookingFee + DistanceKm × PerKmRate) × SurgeMultiplier × CabTypeMultiplier × (1 - SharingDiscount)
```

**Default Values:**
- Booking Fee: ₹25
- Per Km Rate: ₹15
- Minimum Fare: ₹100

**Surge Multipliers:**
- Low demand (<50 active): 1.0x
- Medium demand (50-100): 1.2x
- High demand (100-200): 1.5x
- Very high demand (>200): 2.0x

**Sharing Discounts:**
- Per co-passenger: 5%
- Maximum discount: 25%

**Cab Type Multipliers:**
- SEDAN: 1.0x
- SUV: 1.3x
- VAN: 1.5x
- PREMIUM_SEDAN: 1.8x
