package ui;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import domain.Customer;
import domain.RestockOrder;
import domain.RestockOrderItem;

import javax.swing.*;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PdfGenerator {

    static final String COMPANY_NAME    = "InfoPharma CA Ltd.";
    static final String COMPANY_ADDRESS = "3, High Level Drive";
    static final String COMPANY_CITY    = "Sydenham, SE26 3ET";
    static final String COMPANY_PHONE   = "Phone: 0208 778 0124";
    static final String COMPANY_FAX     = "Fax:   0208 778 0125";

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    // -------------------------------------------------------------------------
    // Order Form
    // -------------------------------------------------------------------------

    public static void generateOrderForm(Component parent,
                                         RestockOrder order,
                                         List<RestockOrderItem> items) {
        File dest = chooseSaveFile(parent, "order-form-" + order.getOrderNumber() + ".pdf");
        if (dest == null) return;

        try {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            PdfDocument pdf = new PdfDocument(new PdfWriter(dest));
            Document doc    = new Document(pdf, PageSize.A4);
            doc.setMargins(50, 50, 50, 50);

            doc.add(para("Client: " + COMPANY_NAME, regular, 11));
            doc.add(para(COMPANY_ADDRESS, regular, 11));
            doc.add(para(COMPANY_CITY, regular, 11));
            doc.add(para(COMPANY_PHONE, regular, 11));
            doc.add(para(COMPANY_FAX, regular, 11));
            doc.add(spacer(8));
            doc.add(para("IPOS Account: " + order.getMerchantId(), regular, 11));
            doc.add(para("Date: " + LocalDate.now().format(DISPLAY_DATE), regular, 11));
            doc.add(spacer(16));

            float[] colWidths = {120f, 200f, 70f, 80f, 80f};
            Table table = new Table(colWidths);
            table.setWidth(UnitValue.createPercentValue(100));

            addHeaderCell(table, "Item ID",      bold);
            addHeaderCell(table, "Description",  bold);
            addHeaderCell(table, "Quantity",     bold);
            addHeaderCell(table, "Unit Cost, £", bold);
            addHeaderCell(table, "Total, £",     bold);

            double grandTotal = 0;
            for (RestockOrderItem item : items) {
                table.addCell(dataCell(item.getItemId(), regular));
                table.addCell(dataCell(item.getDescription(), regular));
                table.addCell(dataCell(String.valueOf(item.getQuantity()), regular));
                table.addCell(dataCell(String.format("%.2f", item.getUnitCost()), regular));
                table.addCell(dataCell(String.format("%.2f", item.getLineTotal()), regular));
                grandTotal += item.getLineTotal();
            }

            Cell labelCell = new Cell(1, 4)
                    .add(new Paragraph("Grand Total:").setFont(bold).setFontSize(11))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(new SolidBorder(0.5f));
            Cell totalCell = new Cell()
                    .add(new Paragraph(String.format("%.2f", grandTotal)).setFont(bold).setFontSize(11))
                    .setBorder(new SolidBorder(0.5f));
            table.addCell(labelCell);
            table.addCell(totalCell);

            doc.add(table);
            doc.add(spacer(24));
            doc.add(para("For " + COMPANY_NAME + ":", regular, 11));
            doc.add(spacer(32));
            doc.add(para("        /Signature/", regular, 11));

            doc.close();
            JOptionPane.showMessageDialog(parent, "Order form saved:\n" + dest.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Failed to generate PDF: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Retail Invoice
    // -------------------------------------------------------------------------

    public static class InvoiceItem {
        public final String description;
        public final int packages;
        public final double packageCost;
        public final double amount;
        public final double vatRate;

        public InvoiceItem(String description, int packages, double packageCost,
                           double amount, double vatRate) {
            this.description = description;
            this.packages    = packages;
            this.packageCost = packageCost;
            this.amount      = amount;
            this.vatRate     = vatRate;
        }
    }

    /**
     * Generates a retail invoice PDF.
     * discountAmount > 0 will show a dedicated Discount line on the invoice.
     */
    public static void generateRetailInvoice(Component parent,
                                             Customer customer,
                                             String invoiceNumber,
                                             List<InvoiceItem> items,
                                             double subtotal,
                                             double discountAmount,
                                             double vatAmount,
                                             double amountDue,
                                             String signedBy) {
        File dest = chooseSaveFile(parent, "invoice-" + invoiceNumber + ".pdf");
        if (dest == null) return;

        try {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            PdfDocument pdf = new PdfDocument(new PdfWriter(dest));
            Document doc    = new Document(pdf, PageSize.A4);
            doc.setMargins(50, 50, 50, 50);

            // Two-column header
            Table header = new Table(new float[]{1f, 1f});
            header.setWidth(UnitValue.createPercentValue(100));

            String custAddr = customer.getFullName() + ",\n"
                    + (customer.getAddress() != null && !customer.getAddress().isEmpty()
                    ? customer.getAddress() : "");
            header.addCell(new Cell()
                    .add(new Paragraph(custAddr).setFont(regular).setFontSize(11))
                    .setBorder(Border.NO_BORDER));

            String compAddr = COMPANY_NAME + ",\n"
                    + COMPANY_ADDRESS + ",\n"
                    + COMPANY_CITY + "\n"
                    + COMPANY_PHONE + "\n"
                    + COMPANY_FAX;
            header.addCell(new Cell()
                    .add(new Paragraph(compAddr).setFont(regular).setFontSize(11)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER));
            doc.add(header);

            doc.add(spacer(16));
            doc.add(para(LocalDate.now().format(DISPLAY_DATE), regular, 11)
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(spacer(16));
            doc.add(para("Dear " + customer.getFullName() + ",", regular, 11));
            doc.add(spacer(12));
            doc.add(para("INVOICE NO.: " + invoiceNumber, bold, 13)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(spacer(8));
            doc.add(para("Account No: " + customer.getAccountNumber(), regular, 11));
            doc.add(spacer(12));

            // Items table
            Table table = new Table(new float[]{200f, 80f, 100f, 100f});
            table.setWidth(UnitValue.createPercentValue(100));

            addHeaderCell(table, "Description",     bold);
            addHeaderCell(table, "Packages",        bold);
            addHeaderCell(table, "Package Cost, £", bold);
            addHeaderCell(table, "Amount, £",       bold);

            for (InvoiceItem item : items) {
                table.addCell(dataCell(item.description, regular));
                table.addCell(dataCell(String.valueOf(item.packages), regular));
                table.addCell(dataCell(String.format("%.2f", item.packageCost), regular));
                table.addCell(dataCell(String.format("%.2f", item.amount), regular));
            }

            // Subtotal row
            addSummaryRow(table, "Subtotal", String.format("%.2f", subtotal), regular, bold);

            // Discount row — only shown if discount > 0
            if (discountAmount > 0) {
                addSummaryRow(table,
                        "Discount Applied",
                        "- " + String.format("%.2f", discountAmount),
                        regular, bold);
            }

            // VAT row
            double displayVatRate = items.isEmpty() ? 0 : items.get(0).vatRate;
            addSummaryRow(table,
                    String.format("VAT @ %.1f%%", displayVatRate),
                    String.format("%.2f", vatAmount),
                    regular, bold);

            // Amount Due
            Cell dueLabelCell = new Cell(1, 3)
                    .add(new Paragraph("Amount Due").setFont(bold).setFontSize(11))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(new SolidBorder(0.5f));
            Cell dueValueCell = new Cell()
                    .add(new Paragraph(String.format("%.2f", amountDue)).setFont(bold).setFontSize(11))
                    .setBorder(new SolidBorder(0.5f));
            table.addCell(dueLabelCell);
            table.addCell(dueValueCell);

            doc.add(table);
            doc.add(spacer(20));
            doc.add(para("Thank you for your valued custom. " +
                    "We look forward to receiving your payment in due course.", regular, 11));
            doc.add(spacer(20));
            doc.add(para("Yours sincerely,", regular, 11));
            doc.add(spacer(40));
            doc.add(para(signedBy, regular, 11));

            doc.close();
            JOptionPane.showMessageDialog(parent, "Invoice saved:\n" + dest.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Failed to generate PDF: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    static File chooseSaveFile(Component parent, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return null;
        File f = chooser.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".pdf")) {
            f = new File(f.getAbsolutePath() + ".pdf");
        }
        return f;
    }

    static Paragraph para(String text, PdfFont font, float size) {
        return new Paragraph(text).setFont(font).setFontSize(size).setMarginBottom(0);
    }

    static Paragraph spacer(float height) {
        return new Paragraph("").setMarginBottom(height);
    }

    static void addHeaderCell(Table table, String text, PdfFont bold) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(text).setFont(bold).setFontSize(11))
                .setBorder(new SolidBorder(0.5f)));
    }

    static Cell dataCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBorder(new SolidBorder(0.5f));
    }

    private static void addSummaryRow(Table table, String label, String value,
                                      PdfFont regular, PdfFont bold) {
        Cell lCell = new Cell(1, 3)
                .add(new Paragraph(label).setFont(regular).setFontSize(11))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(0.5f));
        Cell vCell = new Cell()
                .add(new Paragraph(value).setFont(regular).setFontSize(11))
                .setBorder(new SolidBorder(0.5f));
        table.addCell(lCell);
        table.addCell(vCell);
    }
}