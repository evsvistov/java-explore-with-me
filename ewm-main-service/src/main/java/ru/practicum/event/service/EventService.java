package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.LocationRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.InvalidEventStateException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final EventMapper eventMapper;

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание нового события для пользователя с id: {}", userId);
        User initiator = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(newEventDto.getCategory());

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через два часа от текущего момента");
        }

        Location location = new Location();
        location.setLat(newEventDto.getLocation().getLat());
        location.setLon(newEventDto.getLocation().getLon());
        locationRepository.save(location);

        Event event = eventMapper.toEntity(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setLocation(location);
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);
        log.info("Событие успешно создано: {}", savedEvent);
        return eventMapper.toFullDto(savedEvent);
    }

    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        log.info("Получение событий для пользователя с id: {}", userId);
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<EventShortDto> events = eventRepository.findAllByInitiatorId(userId, pageRequest).stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
        log.info("Найдено {} событий для пользователя с id: {}", events.size(), userId);
        return events;
    }

    public EventFullDto getEventByUser(Long userId, Long eventId) {
        log.info("Получение события с id: {} для пользователя с id: {}", eventId, userId);
        Event event = getEventOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не найдено для данного пользователя");
        }
        log.info("Событие найдено: {}", event);
        return eventMapper.toFullDto(event);
    }

    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события с id: {} для пользователя с id: {}", eventId, userId);
        Event event = getEventOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не найдено для данного пользователя");
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new InvalidEventStateException("Невозможно обновить опубликованное событие");
        }

        updateEventFieldsByUser(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие успешно обновлено: {}", updatedEvent);
        return eventMapper.toFullDto(updatedEvent);
    }

    public List<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                             String rangeStart, String rangeEnd, int from, int size) {
        log.info("Получение событий для администратора");
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<EventState> eventStates = states == null ? null : states.stream()
                .map(EventState::valueOf)
                .collect(Collectors.toList());

        List<EventFullDto> events = eventRepository.findAllByAdmin(users, eventStates, categories, rangeStart, rangeEnd, pageRequest).stream()
                .map(eventMapper::toFullDto)
                .collect(Collectors.toList());
        log.info("Найдено {} событий для администратора", events.size());
        return events;
    }

    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Обновление события с id: {} администратором", eventId);
        Event event = getEventOrThrow(eventId);

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new InvalidEventStateException("Событие может быть опубликовано только если оно в состоянии ожидания");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new InvalidEventStateException("Событие не может быть отклонено, если оно уже опубликовано");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);
        log.info("Событие успешно обновлено администратором: {}", updatedEvent);
        return eventMapper.toFullDto(updatedEvent);
    }

    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd,
                                               Boolean onlyAvailable, String sort, int from, int size) {
        log.info("Получение событий для публичного доступа");
        validateInputParameters(rangeStart, rangeEnd, sort, from, size);
        Sort sorting = createSort(sort);
        PageRequest pageRequest = PageRequest.of(from / size, size, sorting);

        List<Event> events = eventRepository.findAllPublished(text, categories, paid, rangeStart, rangeEnd, pageRequest).getContent();

        if (onlyAvailable) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 || event.getConfirmedRequests() < event.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        List<EventShortDto> eventShortDtos = events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
        log.info("Найдено {} событий для публичного доступа", eventShortDtos.size());
        return eventShortDtos;
    }

    public EventFullDto getEventByIdPublic(Long eventId) {
        log.info("Получение события с id: {} для публичного доступа", eventId);
        Event event = getEventOrThrow(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не найдено");
        }
        event.setViews(event.getViews() + 1);
        Event updatedEvent = eventRepository.save(event);
        log.info("Событие найдено: {}", updatedEvent);
        return eventMapper.toFullDto(updatedEvent);
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + categoryId + " не найдена"));
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(updateRequest.getCategory()));
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата события должна быть не ранее чем через два часа от текущего момента");
            }
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getLocation() != null) {
            event.getLocation().setLat(updateRequest.getLocation().getLat());
            event.getLocation().setLon(updateRequest.getLocation().getLon());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void updateEventFieldsByUser(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(updateRequest.getCategory()));
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата события должна быть не ранее чем через два часа от текущего момента");
            }
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getLocation() != null) {
            event.getLocation().setLat(updateRequest.getLocation().getLat());
            event.getLocation().setLon(updateRequest.getLocation().getLon());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private Sort createSort(String sort) {
        switch (sort.toUpperCase()) {
            case "EVENT_DATE":
                return Sort.by(Sort.Direction.ASC, "eventDate");
            case "VIEWS":
                return Sort.by(Sort.Direction.DESC, "views");
            default:
                return Sort.by(Sort.Direction.ASC, "id");
        }
    }

    private void validateInputParameters(String rangeStart, String rangeEnd, String sort, int from, int size) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if (rangeStart != null && rangeEnd != null) {
            LocalDateTime start = LocalDateTime.parse(rangeStart, formatter);
            LocalDateTime end = LocalDateTime.parse(rangeEnd, formatter);
            if (start.isAfter(end)) {
                throw new BadRequestException("Дата начала должна быть раньше даты окончания");
            }
        }

        if (!sort.equals("EVENT_DATE") && !sort.equals("VIEWS")) {
            throw new BadRequestException("Недопустимое значение для параметра сортировки");
        }

        if (from < 0) {
            throw new BadRequestException("Параметр 'from' должен быть неотрицательным");
        }

        if (size <= 0) {
            throw new BadRequestException("Параметр 'size' должен быть положительным");
        }
    }
}