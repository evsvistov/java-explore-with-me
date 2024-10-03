package ru.practicum.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.event.dto.EventShortDto;

import java.util.Set;

@Data
public class CompilationDto {
    private Long id;
    @Size(min = 1, max = 50, message = "Название подборки должно быть от {min} до {max} символов")
    private String title;
    private Boolean pinned = false;
    private Set<EventShortDto> events;
}