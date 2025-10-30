package ru.practicum.exeption;

public class DataConflictException extends RuntimeException {
    public DataConflictException(String message) {
        super(message);
    }

}
