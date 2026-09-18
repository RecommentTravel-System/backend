package org.example.wayveesystem.mapper;

import org.example.wayveesystem.dto.request.FavoriteRequest;
import org.example.wayveesystem.dto.response.FavoriteResponse;
import org.example.wayveesystem.model.Favorite;
import org.example.wayveesystem.model.User;
import org.springframework.stereotype.Component;

@Component
public class FavoriteMapper {

    public Favorite toFavorite(FavoriteRequest request, User user) {
        if (request == null) {
            return null;
        }
        return Favorite.builder()
                .user(user)
                .osmId(request.getOsmId())
                .placeName(request.getPlaceName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }

    public FavoriteResponse toFavoriteResponse(Favorite favorite) {
        if (favorite == null) {
            return null;
        }
        return FavoriteResponse.builder()
                .favoriteId(favorite.getFavoriteId())
                .userId(favorite.getUser() != null ? favorite.getUser().getUserId() : null)
                .osmId(favorite.getOsmId())
                .placeName(favorite.getPlaceName())
                .latitude(favorite.getLatitude())
                .longitude(favorite.getLongitude())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
