package org.example.wayveesystem.mapper;

import org.example.wayveesystem.dto.request.CategoryRequest;
import org.example.wayveesystem.dto.response.CategoryResponse;
import org.example.wayveesystem.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toCategory(CategoryRequest request) {
        if (request == null) {
            return null;
        }
        return Category.builder()
                .name(request.getName())
                .code(request.getCode())
                .osmKey(request.getOsmKey())
                .osmValue(request.getOsmValue())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .build();
    }

    public CategoryResponse toCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .code(category.getCode())
                .osmKey(category.getOsmKey())
                .osmValue(category.getOsmValue())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .createdAt(category.getCreatedAt())
                .build();
    }

    public void updateCategory(Category category, CategoryRequest request) {
        if (category == null || request == null) {
            return;
        }
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setOsmKey(request.getOsmKey());
        category.setOsmValue(request.getOsmValue());
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
    }
}
