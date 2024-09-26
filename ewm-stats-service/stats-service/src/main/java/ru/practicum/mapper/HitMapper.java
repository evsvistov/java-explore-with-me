package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.model.EndpointHit;

@UtilityClass
public class HitMapper {

    public EndpointHit toEndpointHit(EndpointHitDto hit) {
        return new EndpointHit(
                hit.getApp(),
                hit.getUri(),
                hit.getIp(),
                hit.getTimestamp()
        );
    }

    public EndpointHitDto toEndpointHitDto(EndpointHit hit) {
        return new EndpointHitDto(
                hit.getApp(),
                hit.getUri(),
                hit.getIp(),
                hit.getTimestamp()
        );
    }
}
