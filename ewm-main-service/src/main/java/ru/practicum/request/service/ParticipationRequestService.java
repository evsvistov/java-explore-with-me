package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.RequestConflictException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.ParticipationRequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.ParticipationRequestStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestMapper requestMapper;

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение запросов на участие для пользователя с id: {}", userId);
        List<ParticipationRequestDto> requests = requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
        log.info("Найдено {} запросов на участие для пользователя с id: {}", requests.size(), userId);
        return requests;
    }

    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса на участие для пользователя с id: {} в событии с id: {}", userId, eventId);
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new RequestConflictException("Инициатор не может создать запрос на участие в своем собственном событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new RequestConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new RequestConflictException("Запрос уже существует");
        }

        if (event.getParticipantLimit() != 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new RequestConflictException("Достигнут лимит участников");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequester(requester);
        request.setEvent(event);
        request.setCreated(LocalDateTime.now().withNano(0));

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationRequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Запрос на участие успешно создан: {}", savedRequest);
        return requestMapper.toDto(savedRequest);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса на участие с id: {} для пользователя с id: {}", requestId, userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new RequestConflictException("Пользователь может отменить только свои запросы");
        }

        request.setStatus(ParticipationRequestStatus.CANCELED);
        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Запрос на участие успешно отменен: {}", savedRequest);
        return requestMapper.toDto(savedRequest);
    }

    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        log.info("Получение участников события с id: {} для пользователя с id: {}", eventId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new RequestConflictException("Пользователь не является инициатором события");
        }

        List<ParticipationRequestDto> participants = requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
        log.info("Найдено {} участников события с id: {}", participants.size(), eventId);
        return participants;
    }

    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateRequest) {
        log.info("Изменение статуса запросов на участие в событии с id: {} для пользователя с id: {}", eventId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new BadRequestException("Пользователь не является инициатором события");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new RequestConflictException("Событие не требует модерации запросов");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(statusUpdateRequest.getRequestIds());

        if (requests.size() != statusUpdateRequest.getRequestIds().size()) {
            throw new NotFoundException("Некоторые запросы не найдены");
        }

        int availableSpots = event.getParticipantLimit() - event.getConfirmedRequests();

        if (statusUpdateRequest.getStatus() == ParticipationRequestStatus.CONFIRMED && availableSpots <= 0) {
            throw new RequestConflictException("Достигнут лимит участников");
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(new ArrayList<>());
        result.setRejectedRequests(new ArrayList<>());

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != ParticipationRequestStatus.PENDING) {
                throw new RequestConflictException("Запрос не находится в состоянии ожидания");
            }

            if (statusUpdateRequest.getStatus() == ParticipationRequestStatus.CONFIRMED && availableSpots > 0) {
                request.setStatus(ParticipationRequestStatus.CONFIRMED);
                event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                availableSpots--;
                result.getConfirmedRequests().add(requestMapper.toDto(request));
            } else {
                request.setStatus(ParticipationRequestStatus.REJECTED);
                result.getRejectedRequests().add(requestMapper.toDto(request));
            }
        }

        requestRepository.saveAll(requests);
        eventRepository.save(event);

        if (availableSpots == 0) {
            List<ParticipationRequest> pendingRequests = requestRepository.findAllByEventIdAndStatus(eventId, ParticipationRequestStatus.PENDING);
            pendingRequests.forEach(req -> req.setStatus(ParticipationRequestStatus.REJECTED));
            requestRepository.saveAll(pendingRequests);
            result.getRejectedRequests().addAll(pendingRequests.stream().map(requestMapper::toDto).collect(Collectors.toList()));
        }

        log.info("Статус запросов на участие успешно изменен. Подтверждено: {}, Отклонено: {}",
                result.getConfirmedRequests().size(), result.getRejectedRequests().size());
        return result;
    }
}