package uno.mcme.pnmoneyshop.util;

import java.math.BigDecimal;

public final class NumberParser {

    private NumberParser() {
    }

    public static BigDecimal parseFlexibleDecimal(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        value = value
                .replace(" ", "")
                .replace("￥", "")
                .replace("$", "")
                .replace("金币", "")
                .replace("点券", "");

        String cleaned = value.replaceAll("[^0-9,.-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".") || cleaned.equals(",")) {
            return null;
        }

        int lastDot = cleaned.lastIndexOf('.');
        int lastComma = cleaned.lastIndexOf(',');
        char decimalSeparator = lastDot > lastComma ? '.' : ',';

        if (lastDot >= 0 && lastComma >= 0) {
            cleaned = cleaned.replace(decimalSeparator == '.' ? "," : ".", "");
        }

        if (decimalSeparator == ',') {
            cleaned = cleaned.replace(',', '.');
        }

        cleaned = collapseMinus(cleaned);

        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String collapseMinus(String input) {
        if (input.indexOf('-') <= 0) {
            return input;
        }
        String noMinus = input.replace("-", "");
        return "-" + noMinus;
    }
}
