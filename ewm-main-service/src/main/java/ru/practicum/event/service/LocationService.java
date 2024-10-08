package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.dto.NewLocationDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.mapper.LocationMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.LocationRepository;
import ru.practicum.exception.DuplicateLocationException;
import ru.practicum.exception.LocationInUseException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LocationService {
    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;
    private final LocationMapper locationMapper;
    private final EventMapper eventMapper;

    @Transactional
    public LocationDto createLocation(NewLocationDto newLocationDto) {
        log.info("Создание новой локации: {}", newLocationDto.getName());
        if (locationRepository.existsByName(newLocationDto.getName())) {
            log.warn("Попытка создать локацию с существующим названием: {}", newLocationDto.getName());
            throw new DuplicateLocationException("Локация с таким названием уже существует");
        }
        Location location = locationMapper.toEntity(newLocationDto);
        Location savedLocation = locationRepository.save(location);
        log.info("Локация успешно создана с id: {}", savedLocation.getId());
        return locationMapper.toDto(savedLocation);
    }

    public List<LocationDto> getAllLocations(int from, int size) {
        log.info("Получение списка локаций. From: {}, Size: {}", from, size);
        PageRequest pageRequest = PageRequest.of(from / size, size);

        List<LocationDto> locations = locationRepository.findAll(pageRequest).getContent().stream()
                .map(locationMapper::toDto)
                .collect(Collectors.toList());
        log.info("Получено {} локаций", locations.size());
        return locations;
    }

    public LocationDto getLocationById(Long id) {
        log.info("Получение локации по id: {}", id);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Локация с id {} не найдена", id);
                    return new NotFoundException("Локация с id " + id + " не найдена");
                });
        return locationMapper.toDto(location);
    }

    @Transactional
    public LocationDto updateLocation(Long id, LocationDto updatedLocationDto) {
        log.info("Обновление локации с id: {}", id);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Попытка обновить несуществующую локацию с id: {}", id);
                    return new NotFoundException("Локация не найдена");
                });

        if (updatedLocationDto.getName() != null &&
                !updatedLocationDto.getName().equals(location.getName()) &&
                locationRepository.existsByName(updatedLocationDto.getName())) {
            log.warn("Попытка обновить локацию с уже существующим названием: {}", updatedLocationDto.getName());
            throw new DuplicateLocationException("Локация с таким названием уже существует");
        }

        locationMapper.updateLocationFromDto(updatedLocationDto, location);

        Location updatedLocation = locationRepository.save(location);
        log.info("Локация с id {} успешно обновлена", id);
        return locationMapper.toDto(updatedLocation);
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        log.info("Удаление локации с id: {}", locationId);
        if (!locationRepository.existsById(locationId)) {
            log.warn("Попытка удалить несуществующую локацию с id: {}", locationId);
            throw new NotFoundException("Локация с id " + locationId + " не найдена");
        }
        boolean hasEvents = eventRepository.existsByLocationId(locationId);
        if (hasEvents) {
            log.warn("Попытка удалить локацию с id {}, которая связана с событиями", locationId);
            throw new LocationInUseException("Локация с id " + locationId + " связана с событиями и не может быть удалена");
        }
        locationRepository.deleteById(locationId);
        log.info("Локация с id {} успешно удалена", locationId);
    }

    public List<EventShortDto> getEventsByLocationId(Long locationId, double radius) {
        log.info("Получение событий для локации с id: {} и радиусом: {}", locationId, radius);
        List<Event> events = locationRepository.findByLocationId(locationId, radius);
        List<EventShortDto> eventDtos = events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
        log.info("Найдено {} событий для локации с id: {}", eventDtos.size(), locationId);
        return eventDtos;
    }
}
