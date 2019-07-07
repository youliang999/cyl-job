package com.cyl.job.core.log;

import com.cyl.job.core.biz.model.LogResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;

public class CylJobFileAppender {
    private static Logger logger = LoggerFactory.getLogger(CylJobFileAppender.class);


    // for JobThread (support log for child thread of job handler)
    public static final InheritableThreadLocal<String> contextHolder = new InheritableThreadLocal<>();


    /**
     * log base path
     * struct like
     * <p>
     * --/2019-07-06/log.log
     */
    private static String logBasePath = "/data/applogs/cyl-job/jobhandler";
    private static String glueSrcPath = logBasePath.concat("/gluesource");

    public static void initLogPath(String logPath) {
        //init
        if (logPath != null && logPath.trim().length() > 0) {
            logBasePath = logPath;
        }

        //mk base dir
        File logPathDir = new File(logBasePath);
        if (!logPathDir.exists()) {
            logPathDir.mkdirs();
        }
        logBasePath = logPathDir.getPath();

        //mk glue dir
        File guleBaseDir = new File(logBasePath, "gluesource");
        if (!guleBaseDir.exists()) {
            guleBaseDir.mkdirs();
        }
        glueSrcPath = guleBaseDir.getPath();
    }

    /**
     * log filename, like 'logpath/yyyy-MM-dd/9999.log'
     *
     * @param triggerDate
     * @param logId
     * @return
     */
    public static String makeLogFileName(Date triggerDate, int logId) {
        //filepath/yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        File logFilePath = new File(getLogBasePath(), sdf.format(triggerDate));
        if (!logFilePath.exists()) {
            logFilePath.mkdirs();
        }

        //filepath/yyyy-MM-dd/9999.log
        String logFileName = logFilePath.getPath().concat(File.separator).concat(String.valueOf(logId)).concat(".log");
        return logFileName;
    }

    /**
     * append log
     *
     * @return
     */
    public static void appendLog(String logFileName, String appendLog) {
        // log file
        if (logFileName == null || logFileName.trim().length() == 0) {
            return;
        }
        File logFile = new File(logFileName);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }

        // log
        if (appendLog == null) {
            appendLog = "";
        }
        appendLog += "\r\n";

        //append file content
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(logFile, true);
            fos.write(appendLog.getBytes("utf-8"));
            fos.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * read log file
     * @param logFileName
     * @param fromLineNum
     * @return
     */

    public static LogResult readLog(String logFileName, int fromLineNum) {
        //valid log file
        if (logFileName == null || logFileName.trim().length() > 0) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }
        File logFile = new File(logFileName);
        if (!logFile.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true);
        }

        //read file
        StringBuffer logContentBuffer = new StringBuffer();
        int toLineNum = 0;
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new InputStreamReader(new FileInputStream(logFile), "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                toLineNum = reader.getLineNumber();
                if (toLineNum >= fromLineNum) {
                    logContentBuffer.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        //result
        LogResult logResult = new LogResult(fromLineNum, toLineNum, logContentBuffer.toString(), false);
        return logResult;
    }


    /**
     * read log data
     * @param logFile
     * @return log line content
     */
    public static String readLines(File logFile){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "utf-8"));
            if (reader != null) {
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }

    public static String getLogBasePath() {
        return logBasePath;
    }

    public static String getGlueSrcPath() {
        return glueSrcPath;
    }
}
