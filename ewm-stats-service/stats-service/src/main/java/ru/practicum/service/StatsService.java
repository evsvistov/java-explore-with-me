package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.mapper.HitMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StatsService {
    private final StatsRepository statsRepository;

    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto hitDto) {
        EndpointHit endpointHit = statsRepository.save(HitMapper.toEndpointHit(hitDto));
        return HitMapper.toEndpointHitDto(endpointHit);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) throws BadRequestException {
        if (start.isAfter(end)) {
            throw new BadRequestException("Дата старта не может быть больше даты окнчания");
        }
        if (uris == null || uris.isEmpty()) {
            return unique ? statsRepository.getUniqueStats(start, end) : statsRepository.getStats(start, end);
        } else {
            return unique ? statsRepository.getUniqueStatsByUris(start, end, uris) : statsRepository.getStatsByUris(start, end, uris);
        }
    }
}
