package com.flansmodultimate;

import com.flansmodultimate.platform.PlatformEnvironment;
import com.flansmodultimate.platform.PlatformPaths;
import com.flansmodultimate.util.FlansLog;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/** Waits for the companion packs manager before content discovery begins. */
final class PacksManagerExtraction
{
    private PacksManagerExtraction() {}

    private static final int PACKS_MANAGER_EXTRACTION_STATE_PROTOCOL_VERSION = 1;
    private static final String PACKS_MANAGER_EXTRACTION_STATE_FILE_NAME = ".flansmod_packs_extraction_state.json";
    private static final String PACKS_MANAGER_EXTRACTION_STATE_COMPLETE = "complete";
    private static final String PACKS_MANAGER_EXTRACTION_STATE_FAILED = "failed";
    private static final int TIMEOUT_PACKS_MANAGER_EXTRACTION = 120;

    static void waitForPacksManagerExtractionIfPresent()
    {
        if (!PlatformEnvironment.isModLoaded(FlansMod.PACKS_MANAGER_ID))
            return;

        if (!PlatformEnvironment.isProduction())
        {
            FlansLog.log.info("Flan's Mod Ultimate Packs Manager found, but extraction is disabled outside production. Continuing without waiting.");
            return;
        }

        FlansLog.log.info("Flan's Mod Ultimate Packs Manager found. Waiting for extraction...");

        Path stateFile = PlatformPaths.gameDir().toAbsolutePath().normalize().resolve(PACKS_MANAGER_EXTRACTION_STATE_FILE_NAME);

        long startNanos = System.nanoTime();
        long deadlineNanos = startNanos + TimeUnit.SECONDS.toNanos(TIMEOUT_PACKS_MANAGER_EXTRACTION);
        while (true)
        {
            PacksExtractionWaitState state = readPacksExtractionWaitState(stateFile);
            if (state == PacksExtractionWaitState.COMPLETE)
            {
                FlansLog.log.info("Packs extraction state is complete after waiting {} ms.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
                return;
            }
            if (state == PacksExtractionWaitState.FAILED)
            {
                FlansLog.log.error("Packs extraction failed. Continuing without waiting longer. See the packs extraction state file: {}", stateFile);
                return;
            }
            if (state == PacksExtractionWaitState.UNSUPPORTED)
            {
                FlansLog.log.error("Unsupported packs extraction state file protocol. Continuing without waiting longer: {}", stateFile);
                return;
            }

            if (System.nanoTime() > deadlineNanos)
            {
                FlansLog.log.error("Timed out waiting for packs extraction state to complete: {}", stateFile);
                return;
            }

            // Light sleep to avoid burning CPU
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
        }
    }

    private static PacksExtractionWaitState readPacksExtractionWaitState(Path stateFile)
    {
        if (!Files.isRegularFile(stateFile))
            return PacksExtractionWaitState.WAITING;

        try
        {
            JsonObject object = JsonParser.parseString(Files.readString(stateFile, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!object.has("protocolVersion") || !object.has("state"))
                return PacksExtractionWaitState.WAITING;

            if (object.get("protocolVersion").getAsInt() != PACKS_MANAGER_EXTRACTION_STATE_PROTOCOL_VERSION)
                return PacksExtractionWaitState.UNSUPPORTED;

            String state = object.get("state").getAsString();
            if (PACKS_MANAGER_EXTRACTION_STATE_COMPLETE.equals(state))
                return PacksExtractionWaitState.COMPLETE;
            if (PACKS_MANAGER_EXTRACTION_STATE_FAILED.equals(state))
                return PacksExtractionWaitState.FAILED;

            return PacksExtractionWaitState.WAITING;
        }
        catch (IOException | IllegalStateException | JsonSyntaxException e)
        {
            return PacksExtractionWaitState.WAITING;
        }
    }

    private enum PacksExtractionWaitState
    {
        WAITING,
        COMPLETE,
        FAILED,
        UNSUPPORTED
    }
}
