package ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    public enum ThemeMode {
        LIGHT, DARK
    }

    public interface ThemeListener {
        void applyTheme();
    }

    // START IN DARK MODE
    private static ThemeMode currentMode = ThemeMode.LIGHT;
    private static final List<ThemeListener> listeners = new ArrayList<>();

    public static ThemeMode getCurrentMode() {
        return currentMode;
    }

    public static boolean isDark() {
        return currentMode == ThemeMode.DARK;
    }

    public static void toggleTheme() {
        currentMode = isDark() ? ThemeMode.LIGHT : ThemeMode.DARK;
        notifyListeners();
    }

    public static void register(ThemeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void unregister(ThemeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (ThemeListener listener : new ArrayList<>(listeners)) {
            listener.applyTheme();
        }
    }

    public static Color appBackground() {
        return isDark() ? new Color(15, 18, 24) : new Color(242, 242, 242);
    }

    public static Color panelBackground() {
        return isDark() ? new Color(28, 31, 38) : Color.WHITE;
    }

    public static Color innerCardBackground() {
        return isDark() ? new Color(40, 44, 52) : new Color(245, 245, 245);
    }

    public static Color sidebarBackground() {
        return isDark() ? new Color(10, 12, 18) : new Color(28, 30, 35);
    }

    public static Color sidebarHover() {
        return isDark() ? new Color(45, 50, 60) : new Color(44, 47, 54);
    }

    public static Color sidebarActive() {
        return isDark() ? new Color(67, 74, 88) : new Color(56, 60, 68);
    }

    public static Color topbarBackground() {
        return isDark() ? new Color(24, 27, 34) : new Color(250, 250, 250);
    }

    public static Color borderColor() {
        return isDark() ? new Color(65, 70, 80) : new Color(225, 225, 225);
    }

    public static Color textPrimary() {
        return isDark() ? new Color(238, 238, 238) : new Color(40, 40, 40);
    }

    public static Color textSecondary() {
        return isDark() ? new Color(170, 175, 182) : new Color(120, 120, 120);
    }

    public static Color textLight() {
        return new Color(245, 245, 245);
    }

    public static Color searchBackground() {
        return isDark() ? new Color(42, 46, 54) : new Color(245, 245, 245);
    }

    public static Color buttonDark() {
        return isDark() ? new Color(74, 82, 96) : new Color(30, 32, 38);
    }

    public static Color buttonLight() {
        return isDark() ? new Color(44, 48, 56) : Color.WHITE;
    }

    public static Color tableBackground() {
        return isDark() ? new Color(33, 36, 44) : Color.WHITE;
    }

    public static Color tableHeaderBackground() {
        return isDark() ? new Color(43, 47, 57) : new Color(245, 245, 245);
    }

    public static Color tableGrid() {
        return isDark() ? new Color(65, 70, 80) : new Color(220, 220, 220);
    }

    public static Color comboBackground() {
        return isDark() ? new Color(44, 48, 56) : Color.WHITE;
    }

    public static Color comboForeground() {
        return isDark() ? new Color(230, 230, 230) : new Color(80, 80, 80);
    }

    public static Color fieldBackground() {
        return isDark() ? new Color(42, 46, 54) : Color.WHITE;
    }

    public static Color fieldForeground() {
        return isDark() ? new Color(235, 235, 235) : new Color(45, 45, 45);
    }

    public static Color selectionBackground() {
        return isDark() ? new Color(67, 74, 88) : new Color(235, 235, 235);
    }
}