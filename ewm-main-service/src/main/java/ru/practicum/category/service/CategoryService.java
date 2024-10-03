package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.CategoryInUseException;
import ru.practicum.exception.DuplicateCategoryException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Создание новой категории: {}", newCategoryDto);
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new DuplicateCategoryException("Категория с именем " + newCategoryDto.getName() + " была создана ранее");
        }
        Category category = categoryMapper.toEntity(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);
        log.info("Категория успешно создана: {}", savedCategory);
        return categoryMapper.toDto(savedCategory);
    }

    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории с id: {}", catId);
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Категория с id " + catId + " не существует");
        }
        boolean hasEvents = eventRepository.existsByCategoryId(catId);
        if (hasEvents) {
            throw new CategoryInUseException("Категория с id " + catId + " связана с событиями и не может быть удалена");
        }

        categoryRepository.deleteById(catId);
        log.info("Категория с id {} успешно удалена", catId);
    }

    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Обновление категории с id: {}. Новые данные: {}", catId, categoryDto);
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не существует"));

        if (!category.getName().equals(categoryDto.getName()) && categoryRepository.existsByName(categoryDto.getName())) {
            throw new DuplicateCategoryException("Категория с именем " + categoryDto.getName() + " была создана ранее");
        }

        categoryMapper.updateCategoryFromDto(categoryDto, category);
        Category updatedCategory = categoryRepository.save(category);
        log.info("Категория с id {} успешно обновлена: {}", catId, updatedCategory);
        return categoryMapper.toDto(updatedCategory);
    }

    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Получение списка категорий. From: {}, Size: {}", from, size);
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<CategoryDto> categories = categoryRepository.findAll(pageRequest).getContent().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
        log.info("Получено {} категорий", categories.size());
        return categories;
    }

    public CategoryDto getCategoryById(Long catId) {
        log.info("Получение категории по id: {}", catId);
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не существует"));
        log.info("Категория найдена: {}", category);
        return categoryMapper.toDto(category);
    }
}