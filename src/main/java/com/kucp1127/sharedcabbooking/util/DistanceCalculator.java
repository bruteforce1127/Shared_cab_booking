package com.kucp1127.sharedcabbooking.util;

import com.kucp1127.sharedcabbooking.domain.entity.Location;
import org.springframework.stereotype.Component;

@Component
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    public double calculateDistance(Location from, Location to) {
        if (from == null || to == null) {
            return 0.0;
        }
        return calculateDistance(
                from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude()
        );
    }

    public double calculateDetourPercentage(double directDistance, double newTotalDistance) {
        if (directDistance <= 0) {
            return 0.0;
        }
        return (newTotalDistance - directDistance) / directDistance;
    }

    public double estimateInsertionCost(java.util.List<Location> routePoints, Location newPoint) {
        if (routePoints == null || routePoints.isEmpty()) {
            return 0.0;
        }

        double minAdditionalDistance = Double.MAX_VALUE;

        // Try inserting at each position and find minimum additional distance
        for (int i = 0; i < routePoints.size(); i++) {
            double additionalDistance;

            if (i == 0) {
                // Insert at beginning
                additionalDistance = calculateDistance(newPoint, routePoints.get(0));
            } else if (i == routePoints.size()) {
                // Insert at end
                additionalDistance = calculateDistance(routePoints.get(routePoints.size() - 1), newPoint);
            } else {
                // Insert between two points
                Location prev = routePoints.get(i - 1);
                Location next = routePoints.get(i);
                double currentSegment = calculateDistance(prev, next);
                double newSegmentCost = calculateDistance(prev, newPoint) + calculateDistance(newPoint, next);
                additionalDistance = newSegmentCost - currentSegment;
            }

            minAdditionalDistance = Math.min(minAdditionalDistance, additionalDistance);
        }

        return minAdditionalDistance == Double.MAX_VALUE ? 0.0 : minAdditionalDistance;
    }

    public boolean isWithinRadius(Location point1, Location point2, double radiusKm) {
        return calculateDistance(point1, point2) <= radiusKm;
    }

    public double calculateTotalRouteDistance(java.util.List<Location> stops) {
        if (stops == null || stops.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < stops.size() - 1; i++) {
            totalDistance += calculateDistance(stops.get(i), stops.get(i + 1));
        }
        return totalDistance;
    }
}
