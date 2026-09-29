package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.request.FavoriteRequest;
import org.example.wayveesystem.dto.response.FavoriteResponse;
import org.example.wayveesystem.mapper.FavoriteMapper;
import org.example.wayveesystem.model.Favorite;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.repository.FavoriteRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.FavoriteService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteServiceImpl implements FavoriteService {

    FavoriteRepository favoriteRepository;
    UserRepository userRepository;
    FavoriteMapper favoriteMapper;

    private User getCurrentUser() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(Long.valueOf(userIdStr))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    public FavoriteResponse addFavorite(FavoriteRequest request) {
        User currentUser = getCurrentUser();

        if (favoriteRepository.existsByUserAndOsmId(currentUser, request.getOsmId())) {
            throw new AppException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        Favorite favorite = favoriteMapper.toFavorite(request, currentUser);
        favorite = favoriteRepository.save(favorite);
        return favoriteMapper.toFavoriteResponse(favorite);
    }

    @Override
    public List<FavoriteResponse> getMyFavorites() {
        User currentUser = getCurrentUser();
        return favoriteRepository.findByUser(currentUser).stream()
                .map(favoriteMapper::toFavoriteResponse)
                .toList();
    }

    @Override
    public FavoriteResponse getFavoriteById(Long favoriteId) {
        User currentUser = getCurrentUser();
        Favorite favorite = favoriteRepository.findByFavoriteIdAndUser(favoriteId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_NOT_FOUND));
        return favoriteMapper.toFavoriteResponse(favorite);
    }

    @Override
    public boolean isFavorited(Long osmId) {
        User currentUser = getCurrentUser();
        return favoriteRepository.existsByUserAndOsmId(currentUser, osmId);
    }

    @Override
    @Transactional
    public void deleteFavorite(Long favoriteId) {
        User currentUser = getCurrentUser();
        Favorite favorite = favoriteRepository.findByFavoriteIdAndUser(favoriteId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    @Override
    @Transactional
    public void deleteFavoriteByOsmId(Long osmId) {
        User currentUser = getCurrentUser();
        if (!favoriteRepository.existsByUserAndOsmId(currentUser, osmId)) {
            throw new AppException(ErrorCode.FAVORITE_NOT_FOUND);
        }
        favoriteRepository.deleteByUserAndOsmId(currentUser, osmId);
    }

    @Override
    public List<FavoriteResponse> searchFavorites(String keyword) {
        User currentUser = getCurrentUser();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getMyFavorites();
        }
        return favoriteRepository.findByUserAndPlaceNameContainingIgnoreCase(currentUser, keyword.trim()).stream()
                .map(favoriteMapper::toFavoriteResponse)
                .toList();
    }
}
