package com.example.demo.utils;

import com.example.demo.model.Transaction;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for exporting transaction data to PDF format.
 * Uses iTextPDF library to generate formatted reports.
 */
public class PdfExporter {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,
            new BaseColor(0, 191, 165));
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,
            BaseColor.WHITE);
    private static final Font BODY_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL,
            new BaseColor(50, 50, 50));
    private static final Font TOTAL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,
            new BaseColor(0, 191, 165));

    /**
     * Export a list of transactions to a PDF file at the specified path.
     *
     * @param transactions List of transactions to export
     * @param filePath     Absolute path for the output PDF file
     */
    public static void exportToPdf(List<Transaction> transactions, String filePath) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        // Title
        Paragraph title = new Paragraph("Smart Expense Tracker Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Report date
        Paragraph date = new Paragraph("Generated: " + dateFormat.format(new Date()), BODY_FONT);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(20);
        document.add(date);

        if (transactions == null || transactions.isEmpty()) {
            Paragraph empty = new Paragraph("No transactions found.", BODY_FONT);
            empty.setAlignment(Element.ALIGN_CENTER);
            document.add(empty);
            document.close();
            return;
        }

        // Summary
        double totalIncome = 0, totalExpense = 0;
        for (Transaction t : transactions) {
            if ("income".equals(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        Paragraph summary = new Paragraph("Summary", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD));
        summary.setSpacingBefore(10);
        summary.setSpacingAfter(5);
        document.add(summary);

        document.add(new Paragraph("Total Income: " + currencyFormat.format(totalIncome), TOTAL_FONT));
        document.add(new Paragraph("Total Expenses: " + currencyFormat.format(totalExpense), TOTAL_FONT));
        document.add(new Paragraph("Balance: " + currencyFormat.format(totalIncome - totalExpense), TOTAL_FONT));

        Paragraph space = new Paragraph(" ");
        space.setSpacingAfter(15);
        document.add(space);

        // Transactions table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2, 2, 2, 2});

        // Table headers
        String[] headers = {"Title", "Type", "Category", "Amount", "Date"};
        BaseColor headerBg = new BaseColor(0, 191, 165);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Table body
        for (Transaction t : transactions) {
            table.addCell(createCell(t.getTitle()));
            table.addCell(createCell(t.getType().toUpperCase()));
            table.addCell(createCell(t.getCategory()));
            table.addCell(createCell(currencyFormat.format(t.getAmount())));
            table.addCell(createCell(dateFormat.format(new Date(t.getDate()))));
        }

        document.add(table);
        document.close();
    }

    /**
     * Create a styled table cell.
     */
    private static PdfPCell createCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        return cell;
    }
}
