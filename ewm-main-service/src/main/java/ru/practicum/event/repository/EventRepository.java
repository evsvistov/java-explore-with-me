package ru.practicum.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (TO_TIMESTAMP(:rangeStart, 'YYYY-MM-DD HH24:MI:SS') IS NULL OR e.eventDate >= TO_TIMESTAMP(:rangeStart, 'YYYY-MM-DD HH24:MI:SS')) " +
            "AND (TO_TIMESTAMP(:rangeEnd, 'YYYY-MM-DD HH24:MI:SS') IS NULL OR e.eventDate <= TO_TIMESTAMP(:rangeEnd, 'YYYY-MM-DD HH24:MI:SS'))")
    Page<Event> findAllByAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                               String rangeStart, String rangeEnd, Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text IS NULL OR " +
            "    (CAST(e.annotation AS text) LIKE CONCAT('%', CAST(:text AS text), '%') OR " +
            "     CAST(e.description AS text) LIKE CONCAT('%', CAST(:text AS text), '%'))) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (TO_TIMESTAMP(:rangeStart, 'YYYY-MM-DD HH24:MI:SS') IS NULL OR e.eventDate >= TO_TIMESTAMP(:rangeStart, 'YYYY-MM-DD HH24:MI:SS')) " +
            "AND (TO_TIMESTAMP(:rangeEnd, 'YYYY-MM-DD HH24:MI:SS') IS NULL OR e.eventDate <= TO_TIMESTAMP(:rangeEnd, 'YYYY-MM-DD HH24:MI:SS'))")
    Page<Event> findAllPublished(String text, List<Long> categories, Boolean paid,
                                 String rangeStart, String rangeEnd, Pageable pageable);

    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE e.category.id = :categoryId")
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE e.location.id = :locationId")
    boolean existsByLocationId(Long locationId);
}