package org.example.wayveesystem.service.replanning.distance;

public interface DistanceMatrixCalculator {
    /**
     * Calculates distance in kilometers between two geographic coordinates.
     */
    double distanceKm(double lat1, double lon1, double lat2, double lon2);

    /**
     * Estimates travel duration in minutes between two geographic coordinates.
     */
    int durationMinutes(double lat1, double lon1, double lat2, double lon2);
}
