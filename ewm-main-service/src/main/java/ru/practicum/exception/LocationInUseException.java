package ru.practicum.exception;

public class LocationInUseException extends RuntimeException {
    public LocationInUseException(String message) {
        super(message);
    }
}