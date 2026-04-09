package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

public class TemplatesPanel extends JPanel implements ThemeManager.ThemeListener {

    private static final String REMINDER_1_KEY = "template_reminder_1";
    private static final String REMINDER_2_KEY = "template_reminder_2";
    private static final String INVOICE_KEY    = "template_invoice_header";

    private static final String DEFAULT_REMINDER_1 =
            "Dear {customer_name},\n\n" +
                    "REMINDER - INVOICE NO.: {invoice_number}\n" +
                    "Account: {account_number}    Total Amount: £{amount_due}\n\n" +
                    "According to our records, we have not yet received payment of the above invoice.\n" +
                    "We would appreciate payment at your earliest convenience.\n\n" +
                    "If you have already sent a payment to us recently, please accept our apologies.\n\n" +
                    "Yours sincerely,\n{signed_by}";

    private static final String DEFAULT_REMINDER_2 =
            "Dear {customer_name},\n\n" +
                    "SECOND REMINDER - INVOICE NO.: {invoice_number}\n" +
                    "Account: {account_number}    Total Amount: £{amount_due}\n\n" +
                    "It appears we still have not received payment of the above invoice, despite our previous reminder.\n" +
                    "We would appreciate it if you would settle this invoice in full by return.\n\n" +
                    "If you have already sent a payment to us recently, please accept our apologies.\n\n" +
                    "Yours sincerely,\n{signed_by}";

    private static final String DEFAULT_INVOICE_HEADER =
            "Pharmacy Name: {pharmacy_name}\n" +
                    "Address: {pharmacy_address}\n" +
                    "Phone: {pharmacy_phone}\n" +
                    "Email: {pharmacy_email}";

    private final ScreenRouter router;

    private JTabbedPane tabs;
    private JTextArea reminder1Area;
    private JTextArea reminder2Area;
    private JTextArea invoiceArea;

    public TemplatesPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_TEMPLATES,
                "Templates",
                "Customise reminder and invoice templates",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        loadTemplates();
        applyTheme();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel hint = new JLabel(
                "<html>Available placeholders: <b>{customer_name}</b>, <b>{invoice_number}</b>, " +
                        "<b>{account_number}</b>, <b>{amount_due}</b>, <b>{signed_by}</b>, " +
                        "<b>{pharmacy_name}</b>, <b>{pharmacy_address}</b>, <b>{pharmacy_phone}</b>, <b>{pharmacy_email}</b></html>"
        );
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setBorder(new EmptyBorder(0, 4, 8, 4));

        tabs = new JTabbedPane();

        reminder1Area = createTextArea();
        reminder2Area = createTextArea();
        invoiceArea   = createTextArea();

        tabs.addTab("1st Reminder",       new JScrollPane(reminder1Area));
        tabs.addTab("2nd Reminder",       new JScrollPane(reminder2Area));
        tabs.addTab("Invoice Header",     new JScrollPane(invoiceArea));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);

        JButton resetBtn = new JButton("Reset to Default");
        JButton saveBtn  = new JButton("Save Templates");

        resetBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveBtn.setBackground(ThemeManager.buttonDark());
        saveBtn.setForeground(ThemeManager.textLight());
        saveBtn.setBorder(new EmptyBorder(10, 18, 10, 18));
        saveBtn.setOpaque(true);

        resetBtn.addActionListener(e -> resetCurrentTab());
        saveBtn.addActionListener(e -> saveTemplates());

        buttons.add(resetBtn);
        buttons.add(saveBtn);

        content.add(hint,    BorderLayout.NORTH);
        content.add(tabs,    BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        return content;
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(12, 12, 12, 12));
        return area;
    }

    private void loadTemplates() {
        reminder1Area.setText(loadTemplate(REMINDER_1_KEY, DEFAULT_REMINDER_1));
        reminder2Area.setText(loadTemplate(REMINDER_2_KEY, DEFAULT_REMINDER_2));
        invoiceArea.setText(loadTemplate(INVOICE_KEY, DEFAULT_INVOICE_HEADER));
    }

    private void saveTemplates() {
        saveTemplate(REMINDER_1_KEY, reminder1Area.getText());
        saveTemplate(REMINDER_2_KEY, reminder2Area.getText());
        saveTemplate(INVOICE_KEY,    invoiceArea.getText());
        JOptionPane.showMessageDialog(this, "Templates saved successfully.");
    }

    private void resetCurrentTab() {
        int tab = tabs.getSelectedIndex();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Reset this template to default?", "Confirm Reset", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        switch (tab) {
            case 0 -> { reminder1Area.setText(DEFAULT_REMINDER_1); saveTemplate(REMINDER_1_KEY, DEFAULT_REMINDER_1); }
            case 1 -> { reminder2Area.setText(DEFAULT_REMINDER_2); saveTemplate(REMINDER_2_KEY, DEFAULT_REMINDER_2); }
            case 2 -> { invoiceArea.setText(DEFAULT_INVOICE_HEADER); saveTemplate(INVOICE_KEY, DEFAULT_INVOICE_HEADER); }
        }
    }

    // Persistence using a simple properties file in the working directory
    private static final String PREFS_FILE = "templates.properties";

    private String loadTemplate(String key, String defaultValue) {
        try {
            File f = new File(PREFS_FILE);
            if (!f.exists()) return defaultValue;
            java.util.Properties props = new java.util.Properties();
            try (InputStream in = new FileInputStream(f)) { props.load(in); }
            String val = props.getProperty(key);
            return val != null ? val.replace("\\n", "\n") : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void saveTemplate(String key, String value) {
        try {
            File f = new File(PREFS_FILE);
            java.util.Properties props = new java.util.Properties();
            if (f.exists()) {
                try (InputStream in = new FileInputStream(f)) { props.load(in); }
            }
            props.setProperty(key, value.replace("\n", "\\n"));
            try (OutputStream out = new FileOutputStream(f)) { props.store(out, "IPOS-CA Templates"); }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Static getter so other panels can use the saved templates
    public static String getTemplate(String key, String defaultValue) {
        try {
            File f = new File(PREFS_FILE);
            if (!f.exists()) return defaultValue;
            java.util.Properties props = new java.util.Properties();
            try (InputStream in = new FileInputStream(f)) { props.load(in); }
            String val = props.getProperty(key);
            return val != null ? val.replace("\\n", "\n") : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getReminderTemplate(int type) {
        return type == 1
                ? getTemplate(REMINDER_1_KEY, DEFAULT_REMINDER_1)
                : getTemplate(REMINDER_2_KEY, DEFAULT_REMINDER_2);
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());
        if (reminder1Area != null) { reminder1Area.setBackground(ThemeManager.fieldBackground()); reminder1Area.setForeground(ThemeManager.textPrimary()); }
        if (reminder2Area != null) { reminder2Area.setBackground(ThemeManager.fieldBackground()); reminder2Area.setForeground(ThemeManager.textPrimary()); }
        if (invoiceArea   != null) { invoiceArea.setBackground(ThemeManager.fieldBackground());   invoiceArea.setForeground(ThemeManager.textPrimary()); }
        repaint(); revalidate();
    }
}