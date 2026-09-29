package org.example.wayveesystem.service;

public interface WikimediaService {
    String resolveImage(String commonsFile);
    String searchByNameAndLocation(String name, double lat, double lon);
}
