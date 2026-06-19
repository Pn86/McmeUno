package uno.mcme.pnplayertask.task;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public record RefreshRule(RefreshType type, int minutes) {
    public static RefreshRule parse(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("none")) return new RefreshRule(RefreshType.NONE, 0);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "day" -> new RefreshRule(RefreshType.DAY, 0);
            case "week" -> new RefreshRule(RefreshType.WEEK, 0);
            case "month" -> new RefreshRule(RefreshType.MONTH, 0);
            default -> {
                try { yield new RefreshRule(RefreshType.MINUTES, Math.max(1, Integer.parseInt(normalized))); }
                catch (NumberFormatException ex) { yield new RefreshRule(RefreshType.NONE, 0); }
            }
        };
    }

    public boolean refreshes() { return type != RefreshType.NONE; }

    public long nextAfter(long baseMillis, ZoneId zone) {
        if (!refreshes()) return 0L;
        ZonedDateTime base = Instant.ofEpochMilli(baseMillis).atZone(zone);
        ZonedDateTime next = switch (type) {
            case DAY -> base.toLocalDate().plusDays(1).atStartOfDay(zone);
            case WEEK -> base.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(zone);
            case MONTH -> base.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay(zone);
            case MINUTES -> base.plusMinutes(minutes);
            case NONE -> base;
        };
        return next.toInstant().toEpochMilli();
    }
}
