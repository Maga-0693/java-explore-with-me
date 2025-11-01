package ru.practicum.util;

import lombok.experimental.UtilityClass;
import org.apache.coyote.BadRequestException;
import ru.practicum.events.Dateable;
import ru.practicum.exeption.EmailValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class RequestsValidator {

    public static void dateValidation(Dateable request) throws BadRequestException {

        if (request.getEventDate() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (request.getEventDate().isBefore(now)) {
            throw new BadRequestException(String.format(
                    "Field: eventDate. Error: must contain a date that has not yet occurred. Value: %s",
                    request.getEventDate().format(formatter)));
        } else if (!request.getEventDate().isAfter(now.plusHours(2))) {
            throw new BadRequestException(String.format(
                    "Field: eventDate. Error: Event date must be at least 2 hours from now. Value: %s",
                    request.getEventDate().format(formatter)));
        }
    }

    public static void dateValidation(LocalDateTime localDateTime) throws BadRequestException {

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (localDateTime.isBefore(now)) {
            throw new BadRequestException(String.format(
                    "Field: eventDate. Error: must contain a date that has not yet occurred. Value: %s",
                    localDateTime.format(formatter)));
        } else if (!localDateTime.isAfter(now.plusHours(2))) {
            throw new BadRequestException(String.format(
                    "Field: eventDate. Error: Event date must be at least 2 hours from now. Value: %s",
                    localDateTime.format(formatter)));
        }
    }

    public static void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new EmailValidationException("Field: email. Error: must not be empty. Value: null");
        }

        if (email.length() > 254) {
            throw new EmailValidationException(String.format(
                    "Field: email. Error: max length of email is 254 characters. Value: %d",
                    email.length()));
        }

        String[] atParts = email.split("@");
        if (atParts.length != 2) {
            throw new EmailValidationException(String.format(
                    "Field: email. Error: email should have two parts divided by @. Value: %s",
                    email));
        }

        String localPart = atParts[0];
        String domainPart = atParts[1];

        if (localPart.length() > 64) {
            throw new EmailValidationException(String.format(
                    "Field: email. Error: max size of local part is 64 characters. Value: %d",
                    localPart.length()));
        }

        String[] domainParts = domainPart.split("\\.");

        for (String part : domainParts) {
            if (part.length() > 63) {
                throw new EmailValidationException(String.format(
                        "Field: email. Error: max size of each domain part is 63 characters. Value: %d",
                        part.length()));
            }
        }
    }
}