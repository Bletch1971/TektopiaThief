package bletch.tektopiathief.utils;

import javax.annotation.Nullable;

public class StringUtils {

    public static final String EMPTY = "";

    public static Boolean isNullOrWhitespace(@Nullable String value) {
        return (value == null || value.trim().length() == 0);
    }

}
