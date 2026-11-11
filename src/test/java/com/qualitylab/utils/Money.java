package com.qualitylab.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Reads amounts shown on the page, such as "$1,234.56" or "₱2,500.00", without floating-point errors. */
public final class Money {

    private Money() {
    }

    public static BigDecimal parse(String text) {
        String digits = text.replaceAll("[^0-9.\\-]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("No amount in '" + text + "'.");
        }
        return new BigDecimal(digits).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** The amount as the forms expect it, such as "25.50". */
    public static String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
