package ru.practicum.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateCompilationRequest {

    @Size(min = 1, max = 50, message = "Название подборки должно быть от {min} до {max} символов")
    private String title;

    private Boolean pinned;
    private Set<Long> events;
}