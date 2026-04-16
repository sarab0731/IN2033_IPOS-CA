package app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Central time source for the whole application.
 * All code should call TimeManager.today() / TimeManager.now() instead of
 * LocalDate.now() / LocalDateTime.now() so that demo time-travel works.
 */
public class TimeManager {

    private static Period offset = Period.ZERO;

    /** Current simulated date. */
    public static LocalDate today() {
        return LocalDate.now().plus(offset);
    }

    /** Current simulated date-time (wall-clock time, shifted date). */
    public static LocalDateTime now() {
        return LocalDateTime.now().plus(offset);
    }

    /** Advance the simulated date by the given number of days (may be negative). */
    public static void advanceDays(int days) {
        offset = offset.plusDays(days);
    }

    /** Advance the simulated date by the given number of months. */
    public static void advanceMonths(int months) {
        offset = offset.plusMonths(months);
    }

    /** True if any time offset has been applied. */
    public static boolean isOffsetActive() {
        return !offset.isZero();
    }

    /** Reset back to real time. */
    public static void reset() {
        offset = Period.ZERO;
    }

    /** How far ahead (or behind) the simulated date is, as a human string. */
    public static String offsetLabel() {
        if (offset.isZero()) return "real-time";
        int months = offset.getYears() * 12 + offset.getMonths();
        int days   = offset.getDays();
        StringBuilder sb = new StringBuilder("+");
        if (months != 0) sb.append(months).append("mo ");
        if (days   != 0) sb.append(days).append("d");
        return sb.toString().trim();
    }
}
