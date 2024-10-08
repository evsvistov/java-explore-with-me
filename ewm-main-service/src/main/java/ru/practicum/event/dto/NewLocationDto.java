package ru.practicum.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewLocationDto {
    private String name;
    @NotNull
    private BigDecimal lat;
    @NotNull
    private BigDecimal lon;
    private BigDecimal radius;
}
