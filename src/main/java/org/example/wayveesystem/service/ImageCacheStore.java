package org.example.wayveesystem.service;

public interface ImageCacheStore {
    ImageResolution get(Long osmId);
    void put(Long osmId, ImageResolution resolution);
}
