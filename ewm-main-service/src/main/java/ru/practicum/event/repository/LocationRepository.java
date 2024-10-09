package ru.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("SELECT e FROM Event e " +
            "JOIN e.location l " +
            "WHERE function('distance', e.location.lat, e.location.lon, l.lat, l.lon) <= :radius " +
            "AND l.id = :locationId")
    List<Event> findByLocationId(@Param("locationId") Long locationId,
                                 @Param("radius") double radius);

    boolean existsByName(String name);

    @Query("SELECT e FROM Event e " +
            "JOIN e.location l " +
            "WHERE function('distance', l.lat, l.lon, :lat, :lon) <= :radius ")
    List<Event> findEventsInLocation(Double lat, Double lon, Double radius);

}