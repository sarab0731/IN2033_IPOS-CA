package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        applyGlobalTheme();
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

    public static void applyGlobalTheme() {
        UIManager.put("Panel.background", panelBackground());
        UIManager.put("Viewport.background", tableBackground());
        UIManager.put("ScrollPane.background", tableBackground());

        UIManager.put("Label.foreground", textPrimary());
        UIManager.put("OptionPane.messageForeground", textPrimary());

        UIManager.put("TextField.background", fieldBackground());
        UIManager.put("TextField.foreground", fieldForeground());
        UIManager.put("TextField.caretForeground", fieldForeground());
        UIManager.put("PasswordField.background", fieldBackground());
        UIManager.put("PasswordField.foreground", fieldForeground());
        UIManager.put("PasswordField.caretForeground", fieldForeground());
        UIManager.put("TextArea.background", fieldBackground());
        UIManager.put("TextArea.foreground", fieldForeground());
        UIManager.put("TextArea.caretForeground", fieldForeground());
        UIManager.put("TextPane.background", fieldBackground());
        UIManager.put("TextPane.foreground", fieldForeground());
        UIManager.put("EditorPane.background", fieldBackground());
        UIManager.put("EditorPane.foreground", fieldForeground());

        UIManager.put("ComboBox.background", comboBackground());
        UIManager.put("ComboBox.foreground", comboForeground());
        UIManager.put("ComboBox.selectionBackground", selectionBackground());
        UIManager.put("ComboBox.selectionForeground", textPrimary());

        UIManager.put("List.background", comboBackground());
        UIManager.put("List.foreground", comboForeground());
        UIManager.put("List.selectionBackground", selectionBackground());
        UIManager.put("List.selectionForeground", textPrimary());

        UIManager.put("Table.background", tableBackground());
        UIManager.put("Table.foreground", textPrimary());
        UIManager.put("Table.selectionBackground", selectionBackground());
        UIManager.put("Table.selectionForeground", textPrimary());
        UIManager.put("Table.gridColor", tableGrid());
        UIManager.put("TableHeader.background", tableHeaderBackground());
        UIManager.put("TableHeader.foreground", textPrimary());

        UIManager.put("TabbedPane.background", panelBackground());
        UIManager.put("TabbedPane.foreground", textPrimary());
        UIManager.put("TabbedPane.selected", buttonDark());
        UIManager.put("TabbedPane.contentAreaColor", panelBackground());
        UIManager.put("TabbedPane.focus", borderColor());

        UIManager.put("Button.background", buttonLight());
        UIManager.put("Button.foreground", textPrimary());
        UIManager.put("ToggleButton.background", buttonLight());
        UIManager.put("ToggleButton.foreground", textPrimary());

        UIManager.put("PopupMenu.background", panelBackground());
        UIManager.put("MenuItem.background", panelBackground());
        UIManager.put("MenuItem.foreground", textPrimary());

        UIManager.put("RadioButton.background", appBackground());
        UIManager.put("RadioButton.foreground", textPrimary());
        UIManager.put("CheckBox.background", appBackground());
        UIManager.put("CheckBox.foreground", textPrimary());

        UIManager.put("Separator.foreground", borderColor());
        UIManager.put("Separator.background", borderColor());
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

    public static void styleTextField(JTextField field) {
        if (field == null) return;
        field.setOpaque(true);
        field.setBackground(fieldBackground());
        field.setForeground(fieldForeground());
        field.setCaretColor(fieldForeground());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    public static void styleTextArea(JTextArea area) {
        if (area == null) return;
        area.setOpaque(true);
        area.setBackground(fieldBackground());
        area.setForeground(fieldForeground());
        area.setCaretColor(fieldForeground());
        area.setSelectionColor(selectionBackground());
        area.setSelectedTextColor(textPrimary());
        area.setBorder(BorderFactory.createEmptyBorder());
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        if (comboBox == null) return;
        comboBox.setOpaque(true);
        comboBox.setFocusable(false);
        comboBox.setBackground(comboBackground());
        comboBox.setForeground(comboForeground());
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                new EmptyBorder(4, 8, 4, 8)
        ));

        ListCellRenderer<? super Object> renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );
                label.setBorder(new EmptyBorder(6, 10, 6, 10));
                if (isSelected) {
                    label.setBackground(selectionBackground());
                    label.setForeground(textPrimary());
                } else {
                    label.setBackground(comboBackground());
                    label.setForeground(comboForeground());
                }
                return label;
            }
        };
        @SuppressWarnings("unchecked")
        JComboBox<Object> typedCombo = (JComboBox<Object>) comboBox;
        typedCombo.setRenderer(renderer);
    }

    public static void styleScrollPane(JScrollPane scrollPane, Color viewBackground) {
        if (scrollPane == null) return;
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(true);
        scrollPane.setBackground(viewBackground);
        scrollPane.getViewport().setBackground(viewBackground);
        scrollPane.getViewport().setBorder(null);
    }
}
