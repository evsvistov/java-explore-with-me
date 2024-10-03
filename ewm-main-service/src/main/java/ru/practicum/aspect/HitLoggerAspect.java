package ru.practicum.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.EndpointHitDto;

import java.time.LocalDateTime;

@Aspect
@Component
public class HitLoggerAspect {

    private final StatsClient statsClient;

    public HitLoggerAspect(StatsClient statsClient) {
        this.statsClient = statsClient;
    }

    @Before("@within(ru.practicum.annotation.LogHit) || @annotation(ru.practicum.annotation.LogHit)")
    public void logHit(JoinPoint joinPoint) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        EndpointHitDto hitDto = new EndpointHitDto(
                "ewm-main-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );

        statsClient.saveHit(hitDto);
    }
}
