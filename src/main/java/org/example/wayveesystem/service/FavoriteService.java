package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.FavoriteRequest;
import org.example.wayveesystem.dto.response.FavoriteResponse;

import java.util.List;

public interface FavoriteService {
    FavoriteResponse addFavorite(FavoriteRequest request);
    List<FavoriteResponse> getMyFavorites();
    FavoriteResponse getFavoriteById(Long favoriteId);
    boolean isFavorited(Long osmId);
    void deleteFavorite(Long favoriteId);
    void deleteFavoriteByOsmId(Long osmId);
    List<FavoriteResponse> searchFavorites(String keyword);
}
