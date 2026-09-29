package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.FavoriteRequest;
import org.example.wayveesystem.dto.response.FavoriteResponse;
import org.example.wayveesystem.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favorite API", description = "Endpoints quản lý Danh sách địa điểm yêu thích của Người dùng (Favorite CRUDS)")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteController {

    FavoriteService favoriteService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Thêm địa điểm yêu thích", description = "Lưu một địa điểm vào danh sách yêu thích của người dùng hiện tại")
    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(@Valid @RequestBody FavoriteRequest request) {
        FavoriteResponse response = favoriteService.addFavorite(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Location added to favorites", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Lấy danh sách địa điểm yêu thích của tôi", description = "Lấy danh sách các địa điểm đã yêu thích của người dùng đang đăng nhập")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getMyFavorites() {
        List<FavoriteResponse> favorites = favoriteService.getMyFavorites();
        return ResponseEntity.ok(ApiResponse.success("Get favorites successful", favorites));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Lấy chi tiết item yêu thích theo ID", description = "Lấy chi tiết item favorite theo Favorite ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FavoriteResponse>> getFavoriteById(@PathVariable("id") Long favoriteId) {
        FavoriteResponse response = favoriteService.getFavoriteById(favoriteId);
        return ResponseEntity.ok(ApiResponse.success("Get favorite item successful", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Kiểm tra địa điểm đã được yêu thích chưa", description = "Trạng thái true/false xem địa điểm OSM ID đã nằm trong favorites hay chưa")
    @GetMapping("/check/{osmId}")
    public ResponseEntity<ApiResponse<Boolean>> isFavorited(@PathVariable("osmId") Long osmId) {
        boolean favorited = favoriteService.isFavorited(osmId);
        return ResponseEntity.ok(ApiResponse.success("Check favorite status successful", favorited));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Xóa địa điểm yêu thích theo Favorite ID", description = "Xóa địa điểm khỏi danh sách yêu thích bằng Favorite ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFavorite(@PathVariable("id") Long favoriteId) {
        favoriteService.deleteFavorite(favoriteId);
        return ResponseEntity.ok(ApiResponse.success("Favorite item removed successfully"));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Xóa địa điểm yêu thích theo OSM ID", description = "Xóa địa điểm khỏi danh sách yêu thích bằng OSM ID")
    @DeleteMapping("/osm/{osmId}")
    public ResponseEntity<ApiResponse<Void>> deleteFavoriteByOsmId(@PathVariable("osmId") Long osmId) {
        favoriteService.deleteFavoriteByOsmId(osmId);
        return ResponseEntity.ok(ApiResponse.success("Favorite item removed successfully"));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Tìm kiếm trong danh sách yêu thích", description = "Tìm kiếm các địa điểm yêu thích theo tên địa điểm")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> searchFavorites(
            @RequestParam(name = "keyword", required = false) String keyword) {
        List<FavoriteResponse> responses = favoriteService.searchFavorites(keyword);
        return ResponseEntity.ok(ApiResponse.success("Search favorites successful", responses));
    }
}
