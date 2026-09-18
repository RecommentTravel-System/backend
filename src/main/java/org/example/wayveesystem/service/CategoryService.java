package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.CategoryRequest;
import org.example.wayveesystem.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryById(Long categoryId);
    CategoryResponse getCategoryByCode(String code);
    CategoryResponse updateCategory(Long categoryId, CategoryRequest request);
    void deleteCategory(Long categoryId);
    List<CategoryResponse> searchCategories(String keyword);
}
