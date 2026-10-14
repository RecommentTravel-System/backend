package org.example.wayveesystem.service.replanning.distance;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class HaversineDistanceCalculator implements DistanceMatrixCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Value("${wayvee.routing.average-speed-kmh:30.0}")
    private double averageSpeedKmh = 30.0;

    public HaversineDistanceCalculator() {
    }

    public HaversineDistanceCalculator(double averageSpeedKmh) {
        this.averageSpeedKmh = averageSpeedKmh > 0 ? averageSpeedKmh : 30.0;
    }

    @Override
    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (Double.compare(lat1, lat2) == 0 && Double.compare(lon1, lon2) == 0) {
            return 0.0;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Override
    public int durationMinutes(double lat1, double lon1, double lat2, double lon2) {
        double distKm = distanceKm(lat1, lon1, lat2, lon2);
        if (distKm <= 0.001) {
            return 0;
        }
        double speed = averageSpeedKmh > 0 ? averageSpeedKmh : 30.0;
        double hours = distKm / speed;
        int minutes = (int) Math.ceil(hours * 60.0);
        return Math.max(1, minutes);
    }
}
