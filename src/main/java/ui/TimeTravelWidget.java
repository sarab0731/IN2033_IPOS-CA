package ui;

import app.TimeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/**
 * Compact top-bar widget:  «  ‹  2026-04-16  ›  »  ↺
 *
 *  « / »  — step back / forward one month
 *  ‹ / ›  — step back / forward one day
 *  date   — click to jump to a specific date
 *  ↺      — reset to real today
 */
public class TimeTravelWidget extends JPanel {

    private final JButton prevMonthBtn;
    private final JButton prevDayBtn;
    private final JButton dateBtn;
    private final JButton nextDayBtn;
    private final JButton nextMonthBtn;
    private final JButton resetBtn;
    private final Runnable onChanged;   // called after any time change so parent can react

    public TimeTravelWidget(Runnable onChanged) {
        this.onChanged = onChanged;
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        prevMonthBtn = makeBtn("«");
        prevDayBtn   = makeBtn("‹");
        dateBtn      = makeDateBtn();
        nextDayBtn   = makeBtn("›");
        nextMonthBtn = makeBtn("»");
        resetBtn     = makeBtn("↺");

        prevMonthBtn.setToolTipText("Go back one month");
        prevDayBtn  .setToolTipText("Go back one day");
        nextDayBtn  .setToolTipText("Go forward one day");
        nextMonthBtn.setToolTipText("Go forward one month");
        resetBtn    .setToolTipText("Reset to today");
        dateBtn     .setToolTipText("Click to jump to a specific date");

        prevMonthBtn.addActionListener(e -> { TimeManager.advanceMonths(-1); fireChanged(); });
        prevDayBtn  .addActionListener(e -> { TimeManager.advanceDays(-1);   fireChanged(); });
        nextDayBtn  .addActionListener(e -> { TimeManager.advanceDays( 1);   fireChanged(); });
        nextMonthBtn.addActionListener(e -> { TimeManager.advanceMonths( 1); fireChanged(); });
        resetBtn    .addActionListener(e -> { TimeManager.reset();            fireChanged(); });
        dateBtn     .addActionListener(e -> showJumpDialog());

        add(prevMonthBtn);
        add(prevDayBtn);
        add(dateBtn);
        add(nextDayBtn);
        add(nextMonthBtn);
        add(resetBtn);

        refresh();
    }

    /** Re-reads TimeManager and updates text/colour. Safe to call from any thread via EDT. */
    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            dateBtn.setText(TimeManager.today().toString());
            Color dateColor = TimeManager.isOffsetActive()
                    ? new Color(210, 140, 0)          // amber when shifted
                    : new Color(160, 160, 160);        // neutral grey at real-time
            dateBtn     .setForeground(dateColor);
            prevMonthBtn.setForeground(new Color(160, 160, 160));
            prevDayBtn  .setForeground(new Color(160, 160, 160));
            nextDayBtn  .setForeground(new Color(160, 160, 160));
            nextMonthBtn.setForeground(new Color(160, 160, 160));
            resetBtn    .setForeground(new Color(160, 160, 160));
            repaint();
        });
    }

    /** Override so L&F reinstall (updateComponentTreeUI) re-applies our colours. */
    @Override
    public void updateUI() {
        super.updateUI();
        refresh();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void fireChanged() {
        refresh();
        if (onChanged != null) onChanged.run();
    }

    private void showJumpDialog() {
        LocalDate cur = TimeManager.today();
        JSpinner yearSpinner  = new JSpinner(new SpinnerNumberModel(cur.getYear(), 2020, 2099, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(cur.getMonthValue(), 1, 12, 1));
        JSpinner daySpinner   = new JSpinner(new SpinnerNumberModel(cur.getDayOfMonth(), 1, 31, 1));

        Object[] fields = {
                "Year:",  yearSpinner,
                "Month:", monthSpinner,
                "Day:",   daySpinner
        };

        int result = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                fields, "Jump to Date", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            LocalDate target = LocalDate.of(
                    (int) yearSpinner.getValue(),
                    (int) monthSpinner.getValue(),
                    (int) daySpinner.getValue());
            LocalDate real = LocalDate.now();
            long days = java.time.temporal.ChronoUnit.DAYS.between(real, target);
            TimeManager.reset();
            if (days != 0) TimeManager.advanceDays((int) days);
            fireChanged();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date.");
        }
    }

    private JButton makeBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(new Color(160, 160, 160));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(0, 4, 0, 4));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeDateBtn() {
        JButton btn = new JButton();
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setForeground(new Color(160, 160, 160));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(0, 4, 0, 4));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
