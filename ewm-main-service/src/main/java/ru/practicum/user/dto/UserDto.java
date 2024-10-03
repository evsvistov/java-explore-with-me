package ru.practicum.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;

    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 250, message = "Имя должно содержать от {min} до {max} символов")
    private String name;

    @NotBlank(message = "Email обязателен для заполнения")
    @Email(message = "Email должен быть корректным")
    @Size(min = 6, max = 254, message = "Email должен содержать от {min} до {max} символов")
    private String email;
}
