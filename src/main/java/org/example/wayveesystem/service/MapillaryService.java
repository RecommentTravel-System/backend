package org.example.wayveesystem.service;

public interface MapillaryService {
    String resolveImage(String mapillaryId);
    String searchNearby(double lat, double lon);
}
