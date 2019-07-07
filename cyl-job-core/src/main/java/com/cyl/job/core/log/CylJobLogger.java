package com.cyl.job.core.log;

import com.cyl.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

public class CylJobLogger {
    private static Logger logger = LoggerFactory.getLogger("cyl-job logger");

    /**
     * append log
     */
    private static void logDetail(StackTraceElement callInfo, String appendLog) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
                .append("[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]").append("-")
                .append("[" + callInfo.getLineNumber() + "]").append("-")
                .append("[" + Thread.currentThread().getName() + "]").append(" ")
                .append(appendLog != null ? appendLog : "");
        String formatAppendLog = stringBuffer.toString();

        //appendlog
        String logFileName = CylJobFileAppender.contextHolder.get();
        if (logFileName != null && logFileName.trim().length() > 0) {
            CylJobFileAppender.appendLog(logFileName, formatAppendLog);
        } else {
            logger.info(">>>>>> {}", formatAppendLog);
        }
    }

    /**
     * append log with pattern
     */
    public static void log(String appendLogPattern, Object ... appendLogArguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();
        StackTraceElement callinfo = new Throwable().getStackTrace()[1];
        logDetail(callinfo, appendLog);
    }

    /**
     * append exception stack
     */
    public static void log(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();
        StackTraceElement callinfo = new Throwable().getStackTrace()[1];
        logDetail(callinfo, appendLog);
    }
}
