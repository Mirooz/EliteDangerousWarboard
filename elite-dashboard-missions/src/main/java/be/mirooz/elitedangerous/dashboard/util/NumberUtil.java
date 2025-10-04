package be.mirooz.elitedangerous.dashboard.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberUtil {

    public static String getFormattedNumber(long completedCredits) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.'); // 👉 on définit le séparateur des milliers comme étant le point

        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(completedCredits);
    }
}
