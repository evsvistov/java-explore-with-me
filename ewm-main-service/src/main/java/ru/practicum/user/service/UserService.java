package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.DuplicateEmailException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Добавление нового пользователя: {}", userDto);
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new DuplicateEmailException("Пользователь с email = " + userDto.getEmail() + " уже существует");
        }
        User user = userMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);
        log.info("Пользователь успешно добавлен: {}", savedUser);
        return userMapper.toDto(savedUser);
    }

    public UserDto getUserById(Long id) {
        log.info("Получение пользователя по id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        log.info("Пользователь найден: {}", user);
        return userMapper.toDto(user);
    }

    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.info("Получение списка пользователей. Ids: {}, From: {}, Size: {}", ids, from, size);
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<UserDto> users;
        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findAllByIdIn(ids, pageRequest).stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            users = userRepository.findAll(pageRequest).getContent().stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
        }
        log.info("Получено {} пользователей", users.size());
        return users;
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        log.info("Обновление пользователя с id: {}. Новые данные: {}", id, userDto);
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (userDto.getEmail() != null && !existingUser.getEmail().equals(userDto.getEmail())
                && userRepository.existsByEmail(userDto.getEmail())) {
            throw new DuplicateEmailException("Пользователь с email = " + userDto.getEmail() + " уже существует");
        }

        userMapper.updateUserFromDto(userDto, existingUser);
        User updatedUser = userRepository.save(existingUser);
        log.info("Пользователь успешно обновлен: {}", updatedUser);
        return userMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Удаление пользователя с id: {}", id);
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
        userRepository.deleteById(id);
        log.info("Пользователь с id: {} успешно удален", id);
    }
}
