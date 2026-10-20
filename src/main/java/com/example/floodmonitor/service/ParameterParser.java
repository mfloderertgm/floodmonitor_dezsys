package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.BadRequestException;
import com.example.floodmonitor.model.WarningLevel;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class ParameterParser {

    public static Instant parseTimestamp(String parameterName, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Ungültiges Datumsformat bei '" + parameterName + "': " + text
                    + ". Erwartet wird ISO-8601, zum Beispiel 2026-05-01T08:00:00Z");
        }
    }

    public static WarningLevel parseWarningLevel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return WarningLevel.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Ungültige Warnstufe: " + text
                    + ". Erlaubt sind NORMAL, WARNING, CRITICAL und UNKNOWN");
        }
    }

    public static void checkTimeRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Ungültiger Zeitraum: 'from' liegt nach 'to'");
        }
    }
}
