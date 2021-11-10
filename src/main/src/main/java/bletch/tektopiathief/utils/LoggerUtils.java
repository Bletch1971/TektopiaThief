package bletch.tektopiathief.utils;

import bletch.tektopiathief.TektopiaThief;
import bletch.tektopiathief.core.ModConfig;
import bletch.tektopiathief.core.ModDetails;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;

public class LoggerUtils {

    private static final File debugFile = new File(TektopiaThief.proxy.getMinecraftDirectory() + ModDetails.FILE_DEBUGLOG);
    private static final Logger MOD_LOGGER = LogManager.getLogger(ModDetails.MOD_NAME);

    public static void debug(String message) {
        if (message == null)
            return;

        MOD_LOGGER.debug(message);
        writeLine("[DEBUG] " + message, true);
    }

    public static void debug(String message, Boolean checkConfig) {
        if (message == null)
            return;

        if (checkConfig && !ModConfig.debug.enableDebug)
            return;

        MOD_LOGGER.debug(message);
        writeLine("[DEBUG] " + message, true);
    }

    public static void error(String message) {
        if (message == null)
            return;

        MOD_LOGGER.error(message);
        writeLine("[ERROR] " + message, true);
    }

    public static void fatal(String message) {
        if (message == null)
            return;

        MOD_LOGGER.fatal(message);
        writeLine("[FATAL] " + message, true);
    }

    public static void info(String message) {
        if (message == null)
            return;

        MOD_LOGGER.info(message);
        writeLine("[INFO] " + message, true);
    }

    public static void info(String message, Boolean checkConfig) {
        if (message == null)
            return;

        if (checkConfig && !ModConfig.debug.enableDebug)
            return;

        MOD_LOGGER.info(message);
        writeLine("[INFO] " + message, true);
    }

    public static void trace(String message) {
        if (message == null)
            return;

        MOD_LOGGER.trace(message);
        writeLine("[TRACE] " + message, true);
    }

    public static void warn(String message) {
        if (message == null)
            return;

        MOD_LOGGER.warn(message);
        writeLine("[WARN] " + message, true);
    }

    public static void resetDebug() {
        writeLines(Collections.singletonList("[" + LocalTime.now() + "] " + "Debug Log:"), false);
    }

    public static void writeLine(String line, Boolean append) {
        writeLines(Collections.singletonList("[" + LocalTime.now() + "] " + line), append);
    }

    public static void writeLines(Collection<String> lines, Boolean append) {
        try {
            FileUtils.writeLines(debugFile, lines, append);
        } catch (IOException e) {
        }
    }

}
