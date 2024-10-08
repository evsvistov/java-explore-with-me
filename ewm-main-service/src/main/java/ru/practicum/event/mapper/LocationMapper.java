package ru.practicum.event.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.dto.NewLocationDto;
import ru.practicum.event.model.Location;

@Component
public class LocationMapper {

    public LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }

        LocationDto dto = new LocationDto();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setLat(location.getLat());
        dto.setLon(location.getLon());
        dto.setRadius(location.getRadius());

        return dto;
    }

    public Location toEntity(NewLocationDto newLocationDto) {
        if (newLocationDto == null) {
            return null;
        }

        Location location = new Location();
        location.setName(newLocationDto.getName());
        location.setLat(newLocationDto.getLat());
        location.setLon(newLocationDto.getLon());
        location.setRadius(newLocationDto.getRadius());

        return location;
    }

    public void updateLocationFromDto(LocationDto dto, Location location) {
        if (dto.getName() != null) {
            location.setName(dto.getName());
        }
        if (dto.getLat() != null) {
            location.setLat(dto.getLat());
        }
        if (dto.getLon() != null) {
            location.setLon(dto.getLon());
        }
        if (dto.getRadius() != null) {
            location.setRadius(dto.getRadius());
        }
    }
}
