package ru.practicum.service;

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
public class StatsService {
    private final StatsRepository statsRepository;

    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto hitDto) {
        EndpointHit endpointHit = statsRepository.save(HitMapper.toEndpointHit(hitDto));
        return HitMapper.toEndpointHitDto(endpointHit);
    }

    @Transactional(readOnly = true)
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (uris == null || uris.isEmpty()) {
            return unique ? statsRepository.getUniqueStats(start, end) : statsRepository.getStats(start, end);
        } else {
            return unique ? statsRepository.getUniqueStatsByUris(start, end, uris) : statsRepository.getStatsByUris(start, end, uris);
        }
    }
}
