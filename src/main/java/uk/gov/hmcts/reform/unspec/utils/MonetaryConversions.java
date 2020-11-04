package uk.gov.hmcts.reform.unspec.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static java.util.Objects.requireNonNull;

public class MonetaryConversions {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private MonetaryConversions() {
        // Utilities class, no instances
    }

    public static BigDecimal penniesToPounds(BigInteger amountInPennies) {
        return penniesToPounds(new BigDecimal(amountInPennies));
    }

    public static BigDecimal penniesToPounds(BigDecimal amountInPennies) {
        requireNonNull(amountInPennies);
        return amountInPennies.divide(HUNDRED, 2, RoundingMode.UNNECESSARY);
    }

    public static BigInteger poundsToPennies(BigDecimal amountInPounds) {
        requireNonNull(amountInPounds);
        return amountInPounds.multiply(HUNDRED).toBigInteger();
    }
}
