package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.dto.NewLocationDto;
import ru.practicum.event.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/admin/locations")
@RequiredArgsConstructor
@Slf4j
public class LocationController {
    private final LocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto createLocation(@RequestBody NewLocationDto newlocationDto) {
        return locationService.createLocation(newlocationDto);
    }

    @GetMapping
    public List<LocationDto> getAllLocations(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        return locationService.getAllLocations(from, size);
    }

    @GetMapping("/{id}")
    public LocationDto getLocationById(@PathVariable Long id) {
        return locationService.getLocationById(id);
    }

    @PatchMapping("/{id}")
    public LocationDto updateLocation(@PathVariable Long id, @RequestBody LocationDto locationDto) {
        return locationService.updateLocation(id, locationDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
    }

    @GetMapping("/{locationId}/events")
    public List<EventShortDto> getEventsByLocation(@PathVariable Long locationId,
                                                   @RequestParam Double radius) {
        return locationService.getEventsByLocationId(locationId, radius);
    }
}