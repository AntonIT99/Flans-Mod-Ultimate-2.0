package com.flansmodultimate.util;

import com.flansmodultimate.platform.PlatformPaths;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configures a separate log file for messages emitted by this mod. */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ModLogFile
{
    private static final String LOGGER_NAMESPACE = FlansLog.LOGGER_NAME;
    private static final String INFO_APPENDER_NAME = "FlansModUltimateInfoFile";
    private static final String DEBUG_APPENDER_NAME = "FlansModUltimateDebugFile";

    /**
     * Writes INFO and higher to {@code logs/<modId>.log}, and DEBUG and TRACE
     * to {@code logs/<modId>-debug.log}, alongside the normal Minecraft log.
     */
    public static synchronized void initialize(String modId)
    {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        if (configuration.getAppender(INFO_APPENDER_NAME) != null)
            return;

        Path logDirectory = PlatformPaths.gameDir().resolve("logs");
        try
        {
            Files.createDirectories(logDirectory);

            PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(configuration)
                .withPattern("%d{HH:mm:ss.SSS} [%t/%level] [%logger]: %msg%n%throwable")
                .build();
            FileAppender infoAppender = createAppender(configuration, layout, INFO_APPENDER_NAME,
                logDirectory.resolve(modId + ".log"), true);
            FileAppender debugAppender = createAppender(configuration, layout, DEBUG_APPENDER_NAME,
                logDirectory.resolve(modId + "-debug.log"), false);

            LoggerConfig loggerConfig = configuration.getLoggers().get(LOGGER_NAMESPACE);
            if (loggerConfig == null)
            {
                loggerConfig = new LoggerConfig(LOGGER_NAMESPACE, Level.ALL, true);
                configuration.addLogger(LOGGER_NAMESPACE, loggerConfig);
            }
            loggerConfig.addAppender(infoAppender, null, null);
            loggerConfig.addAppender(debugAppender, null, null);
            context.updateLoggers();
        }
        catch (IOException | RuntimeException exception)
        {
            StatusLogger.getLogger().warn("Unable to create mod log files in {}", logDirectory, exception);
        }
    }

    private static FileAppender createAppender(Configuration configuration, PatternLayout layout,
                                               String name, Path path, boolean infoAndHigher)
    {
        FileAppender appender = FileAppender.newBuilder()
            .setConfiguration(configuration)
            .setName(name)
            .withFileName(path.toString())
            .withAppend(false)
            .setLayout(layout)
            .setFilter(new AbstractFilter()
            {
                @Override
                public Result filter(LogEvent event)
                {
                    boolean isInfoAndHigher = event.getLevel().isMoreSpecificThan(Level.INFO);
                    return isInfoAndHigher == infoAndHigher ? Result.ACCEPT : Result.DENY;
                }
            })
            .build();
        appender.start();
        configuration.addAppender(appender);
        return appender;
    }
}
