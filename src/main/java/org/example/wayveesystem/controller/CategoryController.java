package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.CategoryRequest;
import org.example.wayveesystem.dto.response.CategoryResponse;
import org.example.wayveesystem.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category API", description = "Endpoints quản lý Danh mục địa điểm (Category CRUDS)")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryController {

    CategoryService categoryService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo danh mục mới (Admin)", description = "Tạo một danh mục địa điểm mới trong hệ thống")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", response));
    }

    @Operation(summary = "Lấy tất cả danh mục", description = "Lấy danh sách tất cả danh mục địa điểm")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Get categories successful", categories));
    }

    @Operation(summary = "Lấy chi tiết danh mục theo ID", description = "Lấy thông tin một danh mục theo Category ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable("id") Long categoryId) {
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Get category successful", response));
    }

    @Operation(summary = "Lấy danh mục theo Mã (Code)", description = "Lấy thông tin danh mục dựa vào code unique")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryByCode(@PathVariable("code") String code) {
        CategoryResponse response = categoryService.getCategoryByCode(code);
        return ResponseEntity.ok(ApiResponse.success("Get category by code successful", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật danh mục (Admin)", description = "Cập nhật thông tin danh mục theo ID")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable("id") Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa danh mục (Admin)", description = "Xóa một danh mục theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable("id") Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }

    @Operation(summary = "Tìm kiếm danh mục (Search)", description = "Tìm kiếm danh mục theo tên hoặc mã danh mục")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> searchCategories(
            @RequestParam(name = "keyword", required = false) String keyword) {
        List<CategoryResponse> responses = categoryService.searchCategories(keyword);
        return ResponseEntity.ok(ApiResponse.success("Search categories successful", responses));
    }
}
