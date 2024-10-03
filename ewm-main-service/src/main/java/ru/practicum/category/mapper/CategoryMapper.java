package ru.practicum.category.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.model.Category;

@Component
public class CategoryMapper {

    public CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }

        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }

    public Category toEntity(NewCategoryDto newCategoryDto) {
        if (newCategoryDto == null) {
            return null;
        }

        Category category = new Category();
        category.setName(newCategoryDto.getName());
        return category;
    }

    public void updateCategoryFromDto(CategoryDto categoryDto, Category category) {
        if (categoryDto.getName() != null && !categoryDto.getName().equals(category.getName())) {
            category.setName(categoryDto.getName());
        }
    }
}