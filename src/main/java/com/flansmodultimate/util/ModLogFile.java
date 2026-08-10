package com.flansmodultimate.util;

import lombok.NoArgsConstructor;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configures a separate log file for messages emitted by this mod. */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ModLogFile
{
    private static final String LOGGER_NAMESPACE = "com.flansmodultimate";
    private static final String APPENDER_NAME = "FlansModUltimateFile";

    /**
     * Writes this mod's log messages to {@code logs/<modId>.log} as well as the
     * normal Minecraft log.
     */
    public static synchronized void initialize(String modId)
    {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        if (configuration.getAppender(APPENDER_NAME) != null)
            return;

        Path logFile = FMLPaths.GAMEDIR.get().resolve("logs").resolve(modId + ".log");
        try
        {
            Files.createDirectories(logFile.getParent());

            PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(configuration)
                .withPattern("%d{HH:mm:ss.SSS} [%t/%level] [%logger]: %msg%n%throwable")
                .build();
            FileAppender appender = FileAppender.newBuilder()
                .setConfiguration(configuration)
                .setName(APPENDER_NAME)
                .withFileName(logFile.toString())
                .withAppend(true)
                .setLayout(layout)
                .build();
            appender.start();
            configuration.addAppender(appender);

            LoggerConfig loggerConfig = configuration.getLoggers().get(LOGGER_NAMESPACE);
            if (loggerConfig == null)
            {
                loggerConfig = new LoggerConfig(LOGGER_NAMESPACE, Level.ALL, true);
                configuration.addLogger(LOGGER_NAMESPACE, loggerConfig);
            }
            loggerConfig.addAppender(appender, null, null);
            context.updateLoggers();
        }
        catch (IOException | RuntimeException exception)
        {
            StatusLogger.getLogger().warn("Unable to create mod log file {}", logFile, exception);
        }
    }
}
