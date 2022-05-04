package io.github.braully.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author braully
 */
public class UtilParse {

    public static List<String> parseList(String str, String delimit) {
        String[] excludes = new String[]{str};
        if (str.contains(delimit)) {
            excludes = str.split(delimit);
        }
        return Arrays.asList(excludes);
    }

    public static BigDecimal parseBigDecimal(String strValor, int scale) {
        if (strValor == null) {
            return null;
        }
        String strValorTrimed = UtilString.removeLeftZeros(UtilString.numbers(strValor));
        long parseLong = Long.parseLong(strValorTrimed);
        return BigDecimal.valueOf(parseLong, scale);
    }
}
