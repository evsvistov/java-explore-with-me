package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private Long id;
    private String name;
    private BigDecimal lat;
    private BigDecimal lon;
    private BigDecimal radius;

    public LocationDto(BigDecimal lat, BigDecimal lon) {
        this.lat = lat;
        this.lon = lon;
    }
}
