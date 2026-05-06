package com.example.demo.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw OCR text extracted from a receipt image.
 * Extracts: merchant name, total amount, date, and auto-detects expense category.
 */
public class ReceiptParser {

    // ─── Result Model ────────────────────────────────────────────────────────

    public static class ParsedReceipt {
        public String merchantName = "";
        public double amount = 0.0;
        public long dateMillis = System.currentTimeMillis();
        public String category = "Shopping";
        public String notes = "";
        public boolean amountFound = false;
        public boolean dateFound = false;
    }

    // ─── Regex Patterns ──────────────────────────────────────────────────────

    // Matches: ₹450, Rs 450, INR 450, Total: 450, Grand Total 450.00, TOTAL 450
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?:total|grand\\s*total|amount|payable|due|net|subtotal|balance)[\\s:]*[₹$€£]?\\s*(\\d{1,7}(?:[.,]\\d{1,2})?)" +
        "|[₹$€£]\\s*(\\d{1,7}(?:[.,]\\d{1,2})?)" +
        "|(?:rs|inr|usd)[.\\s]*(\\d{1,7}(?:[.,]\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE
    );

    // Matches: 30/04/2025, 30-04-2025, 2025-04-30, April 30 2025, 30 Apr 2025
    private static final Pattern DATE_PATTERN_DMY = Pattern.compile(
        "(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{4})"
    );
    private static final Pattern DATE_PATTERN_YMD = Pattern.compile(
        "(\\d{4})[/\\-.](\\d{1,2})[/\\-.](\\d{1,2})"
    );

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Parse all useful fields from raw OCR text.
     */
    public static ParsedReceipt parse(String rawText) {
        ParsedReceipt result = new ParsedReceipt();
        if (rawText == null || rawText.trim().isEmpty()) return result;

        String text = rawText.trim();

        result.merchantName = extractMerchantName(text);
        result.amount       = extractAmount(text);
        result.amountFound  = result.amount > 0;
        result.dateMillis   = extractDate(text);
        result.dateFound    = result.dateMillis != -1;
        if (!result.dateFound) result.dateMillis = System.currentTimeMillis();
        result.category     = categorizeExpense(result.merchantName, text);
        result.notes        = extractNotes(text);

        return result;
    }

    // ─── Merchant Name ───────────────────────────────────────────────────────

    public static String extractMerchantName(String text) {
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // Skip very short lines, pure numbers, and common receipt noise
            if (trimmed.length() < 3) continue;
            if (trimmed.matches("[\\d\\s\\-\\.,:;/]+")) continue;
            if (trimmed.toLowerCase().matches(".*(tel|ph:|phone|www|http|gst|tax|bill|receipt|invoice).*")) continue;
            // Take the first clean line as merchant
            return toTitleCase(trimmed);
        }
        return "Bill/Receipt";
    }

    // ─── Amount ──────────────────────────────────────────────────────────────

    /**
     * Finds all amounts and returns the largest (most likely the Grand Total).
     */
    public static double extractAmount(String text) {
        List<Double> found = new ArrayList<>();
        Matcher m = AMOUNT_PATTERN.matcher(text);
        while (m.find()) {
            // Try each capture group
            for (int g = 1; g <= m.groupCount(); g++) {
                String val = m.group(g);
                if (val != null && !val.isEmpty()) {
                    try {
                        double parsed = Double.parseDouble(val.replace(",", ""));
                        if (parsed > 0 && parsed < 1_000_000) found.add(parsed);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Fallback: any plain number that looks like a price
        if (found.isEmpty()) {
            Pattern fallback = Pattern.compile("(?<![\\d.])\\d{2,6}(?:\\.\\d{1,2})?(?![\\d])");
            Matcher fm = fallback.matcher(text);
            while (fm.find()) {
                try {
                    double v = Double.parseDouble(fm.group());
                    if (v > 0 && v < 100_000) found.add(v);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (found.isEmpty()) return 0.0;
        // Return the largest value found (usually Total / Grand Total)
        double max = 0;
        for (double d : found) if (d > max) max = d;
        return max;
    }

    // ─── Date ────────────────────────────────────────────────────────────────

    /**
     * Returns date as epoch millis, or -1 if not found.
     */
    public static long extractDate(String text) {
        // Try DD/MM/YYYY or DD-MM-YYYY
        Matcher m1 = DATE_PATTERN_DMY.matcher(text);
        if (m1.find()) {
            try {
                int day   = Integer.parseInt(m1.group(1));
                int month = Integer.parseInt(m1.group(2));
                int year  = Integer.parseInt(m1.group(3));
                if (isValidDate(day, month, year)) {
                    return buildMillis(day, month, year);
                }
            } catch (NumberFormatException ignored) {}
        }
        // Try YYYY-MM-DD (ISO)
        Matcher m2 = DATE_PATTERN_YMD.matcher(text);
        if (m2.find()) {
            try {
                int year  = Integer.parseInt(m2.group(1));
                int month = Integer.parseInt(m2.group(2));
                int day   = Integer.parseInt(m2.group(3));
                if (isValidDate(day, month, year)) {
                    return buildMillis(day, month, year);
                }
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    // ─── Category ────────────────────────────────────────────────────────────

    public static String categorizeExpense(String merchant, String fullText) {
        String combined = (merchant + " " + fullText).toLowerCase(Locale.ROOT);

        // Food & Dining
        if (combined.matches(".*\\b(dominos?|domino|pizza|kfc|mcdonald|mcdonalds|burger|subway|zomato|swiggy|" +
                "biryani|restaurant|cafe|dhaba|eatery|bakery|food|meal|dine|tea|coffee|starbucks|dunkin)\\b.*"))
            return "Food";

        // Travel & Transport
        if (combined.matches(".*\\b(bpcl|hpcl|hp petrol|iocl|petrol|diesel|fuel|ola|uber|cab|auto|taxi|" +
                "railway|irctc|flight|airline|indigo|spicejet|metro|bus|toll|parking|transport)\\b.*"))
            return "Travel";

        // Bills & Utilities
        if (combined.matches(".*\\b(electricity|water|gas|internet|broadband|airtel|jio|bsnl|vi|vodafone|" +
                "recharge|mobile|postpaid|prepaid|bill|utility|insurance|emi|loan|rent)\\b.*"))
            return "Bills";

        // Health & Medical
        if (combined.matches(".*\\b(hospital|clinic|pharmacy|medicine|medical|doctor|health|apollo|" +
                "fortis|max hospital|lab|diagnostic|chemist|drug|test)\\b.*"))
            return "Health";

        // Entertainment
        if (combined.matches(".*\\b(cinema|pvr|inox|netflix|amazon prime|hotstar|spotify|game|" +
                "entertainment|movie|theatre|concert|event|ticket|bookmyshow)\\b.*"))
            return "Entertainment";

        // Shopping (Grocery + Retail)
        if (combined.matches(".*\\b(bigbazaar|big bazaar|dmart|d-mart|reliance|mall|supermarket|" +
                "grocery|amazon|flipkart|myntra|retail|shop|market|store|fashion|cloth|apparel)\\b.*"))
            return "Shopping";

        return "Shopping"; // Default
    }

    // ─── Notes ───────────────────────────────────────────────────────────────

    /**
     * Extracts item lines (looks for lines with qty/price pattern) for the notes field.
     * Limits to first 5 items to keep notes brief.
     */
    private static String extractNotes(String text) {
        String[] lines = text.split("\\n");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        Pattern itemLine = Pattern.compile("^.{3,40}\\s+\\d+(?:\\.\\d{1,2})?$");
        for (String line : lines) {
            String t = line.trim();
            if (itemLine.matcher(t).matches() && count < 5) {
                sb.append(t).append("\n");
                count++;
            }
        }
        return sb.toString().trim();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static boolean isValidDate(int day, int month, int year) {
        return year >= 2000 && year <= 2030
            && month >= 1 && month <= 12
            && day >= 1 && day <= 31;
    }

    private static long buildMillis(int day, int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static String toTitleCase(String input) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : input.toCharArray()) {
            sb.append(nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            nextUpper = (c == ' ');
        }
        return sb.toString();
    }
}
