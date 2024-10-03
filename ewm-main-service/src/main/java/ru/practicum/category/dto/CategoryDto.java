package ru.practicum.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryDto {
    private Long id;

    @NotBlank
    @Size(min = 1, max = 50, message = "Название категории должно быть от {min} до {max} символов")
    private String name;
}