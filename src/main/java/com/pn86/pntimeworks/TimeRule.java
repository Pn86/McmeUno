package com.pn86.pntimeworks;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

public record TimeRule(RuleType type, int dayValue, int hour, int minute, String raw) {

    public static Optional<TimeRule> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        String trimmed = raw.trim();
        String[] parts = trimmed.split(";");
        if (parts.length != 2) {
            return Optional.empty();
        }

        String dayPart = parts[0].trim();
        String timePart = parts[1].trim();
        String[] hm = timePart.split(":");
        if (hm.length != 2) {
            return Optional.empty();
        }

        int hour;
        int minute;
        try {
            hour = Integer.parseInt(hm[0]);
            minute = Integer.parseInt(hm[1]);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }

        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return Optional.empty();
        }

        if ("0".equals(dayPart)) {
            return Optional.of(new TimeRule(RuleType.DAILY, 0, hour, minute, trimmed));
        }

        if (dayPart.matches("[1-7]")) {
            int weeklyDay = Integer.parseInt(dayPart);
            return Optional.of(new TimeRule(RuleType.WEEKLY, weeklyDay, hour, minute, trimmed));
        }

        if (dayPart.matches("\\d{2}")) {
            int monthDay = Integer.parseInt(dayPart);
            if (monthDay >= 1 && monthDay <= 31) {
                return Optional.of(new TimeRule(RuleType.MONTHLY, monthDay, hour, minute, trimmed));
            }
        }

        return Optional.empty();
    }

    public boolean matches(LocalDateTime now) {
        if (now.getHour() != hour || now.getMinute() != minute) {
            return false;
        }

        return switch (type) {
            case DAILY -> true;
            case WEEKLY -> mapDayOfWeek(now.getDayOfWeek()) == dayValue;
            case MONTHLY -> now.getDayOfMonth() == dayValue;
        };
    }

    public String uniqueKey() {
        return type.name().toLowerCase(Locale.ROOT) + "-" + dayValue + "-" + hour + "-" + minute;
    }

    private int mapDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
            case SUNDAY -> 7;
        };
    }

    public enum RuleType {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}
