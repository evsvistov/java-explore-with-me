package ru.practicum.request.dto;

import lombok.Data;
import ru.practicum.request.model.ParticipationRequestStatus;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;
    private ParticipationRequestStatus status;
}
