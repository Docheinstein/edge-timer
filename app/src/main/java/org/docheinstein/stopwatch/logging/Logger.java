package org.docheinstein.stopwatch.logging;

import android.content.Context;
import android.util.Log;

import org.docheinstein.stopwatch.utils.TimeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    public enum Level {
        Error("[E]", Log.ERROR),
        Warn("[W]", Log.WARN),
        Info("[I]", Log.INFO),
        Debug("[D]", Log.DEBUG),
        ;

        Level(String prefix, int logcatPriority) {
            this.prefix = prefix;
            this.logcatPriority = logcatPriority;
        }

        private String prefix;
        private int logcatPriority;
    }
    private static final String TAG = "Logger";

    private static final String LOGS_FOLDER = "logs";

    private static Logger sLogger;

    private boolean mWriteOnLogcat = true;
    private boolean mWriteOnFile = false;
    private boolean mFlushOnWrite = true;
    private BufferedWriter mWriter;

    public static Logger getInstance(Context context) {
        if (sLogger == null)
            sLogger = new Logger();
        sLogger.initFileWriterIfNeeded(context);
        return sLogger;
    }

    private Logger() {}

    private void initFileWriterIfNeeded(Context context) {
        if (!mWriteOnFile)
            return;

        if (mWriter != null)
            return;

        if (context == null) {
            Log.w(TAG, "Creation failed due to null context");
            return;
        }

        File appDir = context.getExternalFilesDir(null);

        Log.d(TAG, "App directory: '" + appDir + "'");

        File logsDir = new File(appDir, LOGS_FOLDER);
        Log.d(TAG, "Logs directory: '" + logsDir + "'");

        if (!logsDir.exists()) {
            Log.i(TAG, "Creating '" + logsDir + "'");
            try {
                logsDir.mkdirs();
            } catch (Exception e) {
                Log.e(TAG, "Creation of '" + logsDir + "' failed");
                e.printStackTrace();
                return;
            }
        }

        File logFile = new File(logsDir, currentLogFilename());
        Log.d(TAG, "Log file: '" + logFile + "'");

        if (!logFile.exists()) {
            Log.i(TAG, "Creating '" + logFile + "'");
            try {
                logFile.createNewFile();
            } catch (Exception e) {
                Log.e(TAG, "Creation of '" + logFile + "' failed");
                e.printStackTrace();
                return;
            }
        }

        if (!logFile.exists()) {
            Log.e(TAG, "Creation of '" + logFile + "' failed");
            return;
        }

        try {
            mWriter = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            Log.e(TAG, "Creation of BufferWriter for log file failed");
        }

        d(TAG, "Log writer has been created" + "\n" + stackTraceToString(Thread.currentThread().getStackTrace()));
    }

    public void enableFlushOnWrite(boolean enabled) {
        mFlushOnWrite = enabled;
    }

    public void enableWriteOnLogcat(boolean enabled) {
        mWriteOnLogcat = enabled;
    }

    public void enableWriteOnFile(boolean enabled) {
        mWriteOnFile = enabled;
    }

    public void etrace(String tag, String s) {
        e(tag, s + "\n" + stackTraceToString(Thread.currentThread().getStackTrace()));
    }

    public void wtrace(String tag, String s) {
        w(tag, s + "\n" + stackTraceToString(Thread.currentThread().getStackTrace()));
    }

    public void e(String tag, String s) {
        write(Level.Error, tag, s);
    }

    public void w(String tag, String s) {
        write(Level.Warn, tag, s);
    }

    public void i(String tag, String s) {
        write(Level.Info, tag, s);
    }

    public void d(String tag, String s) {
        write(Level.Debug, tag, s);
    }

    public void write(Level lv, String tag, String s) {
        if (mWriteOnLogcat) {
            Log.println(lv.logcatPriority, tag, s);
        }

        if (mWriteOnFile) {
            if (mWriter != null) {
                try {
                    mWriter.write(TimeUtils.getCurrentDatetime("yyyy-MM-DD HH:mm::ss") +
                            " " + lv.prefix + " " + tag + ": " + s + "\n"
                    );
                } catch (IOException e) {
                    Log.w(TAG, "Failed to write to log file");
                }

                if (mFlushOnWrite)
                    flush();
            } else {
                Log.w(TAG, "Can't log to file; mWriter is null");
            }
        }
    }

    public void flush() {
        if (mWriter == null) {
            Log.w(TAG, "Can't flush to file; mWriter is null");
            return;
        }

        try {
            mWriter.flush();
        } catch (IOException e) {
            Log.w(TAG, "Failed to flush log file");
        }
    }

    private static String stackTraceToString(StackTraceElement[] stackEntries) {
        StringBuilder ss = new StringBuilder("========== STACK ==========\n");

        int indent = 0;
        for (int i = stackEntries.length - 2; i >= 0; i--) {
            StackTraceElement ste = stackEntries[i];

            for (int j = 0; j < indent; j++)
                ss.append(" ");

            ss.append(ste);
            ss.append("\n");

            indent++;
        }

        ss.append("===========================");

        return ss.toString();
    }

    private static String currentLogFilename() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return fmt.format(new Date()) + ".log";
    }
}
