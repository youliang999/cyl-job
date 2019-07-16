package com.cyl.api.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ThrowableUtil {
    public ThrowableUtil() {
    }

    public static String toString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String errorMsg = stringWriter.toString();
        return errorMsg;
    }
}
