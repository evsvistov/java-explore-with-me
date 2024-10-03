package ru.practicum.compilation.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.mapper.EventMapper;

import java.util.stream.Collectors;

@Component
public class CompilationMapper {
    private final EventMapper eventMapper;

    public CompilationMapper(EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public CompilationDto toDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setTitle(compilation.getTitle());
        dto.setPinned(compilation.getPinned());
        dto.setEvents(compilation.getEvents().stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toSet()));
        return dto;
    }

    public Compilation toEntity(NewCompilationDto newCompilationDto) {
        if (newCompilationDto == null) {
            return null;
        }

        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setPinned(newCompilationDto.getPinned());
        return compilation;
    }

    public void updateCompilationFromDto(UpdateCompilationRequest updateRequest, Compilation compilation) {
        if (updateRequest == null || compilation == null) {
            return;
        }

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }
    }
}