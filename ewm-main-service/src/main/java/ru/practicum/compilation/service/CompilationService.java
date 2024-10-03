package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Создание новой подборки: {}", newCompilationDto);
        Compilation compilation = compilationMapper.toEntity(newCompilationDto);
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            compilation.setEvents(new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents())));
        } else {
            compilation.setEvents(new HashSet<>());
        }
        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Подборка успешно создана: {}", savedCompilation);
        return compilationMapper.toDto(savedCompilation);
    }

    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с id: {}", compId);
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id " + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
        log.info("Подборка с id {} успешно удалена", compId);
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        log.info("Обновление подборки с id: {}. Новые данные: {}", compId, updateRequest);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));
        compilationMapper.updateCompilationFromDto(updateRequest, compilation);
        if (updateRequest.getEvents() != null) {
            compilation.setEvents(new HashSet<>(eventRepository.findAllById(updateRequest.getEvents())));
        }
        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка с id {} успешно обновлена: {}", compId, updatedCompilation);
        return compilationMapper.toDto(updatedCompilation);
    }

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение списка подборок. Pinned: {}, From: {}, Size: {}", pinned, from, size);
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<CompilationDto> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageRequest).stream()
                    .map(compilationMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            compilations = compilationRepository.findAll(pageRequest).stream()
                    .map(compilationMapper::toDto)
                    .collect(Collectors.toList());
        }
        log.info("Получено {} подборок", compilations.size());
        return compilations;
    }

    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки по id: {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));
        log.info("Подборка найдена: {}", compilation);
        return compilationMapper.toDto(compilation);
    }
}

