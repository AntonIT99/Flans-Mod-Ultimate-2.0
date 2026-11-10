package com.flansmodultimate.client.eras;

import com.flansmodultimate.common.eras.EraState;
import com.flansmodultimate.common.eras.EraVerdicts;
import com.flansmodultimate.common.eras.ServicePeriod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.network.client.eras.PacketEraState;
import com.flansmodultimate.util.FlansLog;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Connection-scoped mirror of the server's {@link EraState} for this player. It decides only what the client shows, such
 * as hidden creative entries and greyed slots; the server refuses blocked actions on its own. Client thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EraClientState
{
    private static final StringBuilder incoming = new StringBuilder();
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static EraState state = EraState.EMPTY;
    private static EraVerdicts verdicts = newVerdicts(EraState.EMPTY);
    @Nullable
    private static UUID transfer;
    private static int nextChunk;
    private static int chunkCount;

    public static void accept(UUID id, int index, int count, String text)
    {
        if (count > PacketEraState.MAX_CHUNKS || index < 0 || index >= count)
            return;
        if (index == 0)
        {
            transfer = id;
            nextChunk = 0;
            chunkCount = count;
            incoming.setLength(0);
        }
        if (!id.equals(transfer) || index != nextChunk || count != chunkCount)
            return;
        incoming.append(text);
        nextChunk++;
        if (nextChunk != count)
            return;
        try
        {
            install(EraState.fromJson(JsonParser.parseString(incoming.toString()).getAsJsonObject()));
        }
        catch (RuntimeException exception)
        {
            FlansLog.log.warn("Ignoring an unreadable era state from the server", exception);
        }
        incoming.setLength(0);
        transfer = null;
    }

    public static EraState state()
    {
        return state;
    }

    /** Whether the local player may have and use the type, as far as the client knows. */
    public static EraVerdicts.Verdict check(InfoType type)
    {
        if (state.bypass())
            return EraVerdicts.Verdict.ALLOWED;
        return verdicts.check(state.restrictions(), type);
    }

    /** Verdicts for the server's eras and dates, for judging restrictions other than the player's own. */
    public static EraVerdicts verdicts()
    {
        return verdicts;
    }

    @Nullable
    public static ServicePeriod periodOf(InfoType type)
    {
        return verdicts.periodOf(type);
    }

    /** Runs after every change, for views that must be rebuilt, such as creative tabs. */
    public static void addListener(Runnable listener)
    {
        listeners.add(listener);
    }

    /** Forgets the server's state when the connection ends. */
    public static void clear()
    {
        incoming.setLength(0);
        transfer = null;
        install(EraState.EMPTY);
    }

    private static void install(EraState received)
    {
        // Logins and dimension changes often resend what the client has; views are rebuilt only for a real change.
        if (received.equals(state))
            return;
        state = received;
        verdicts = newVerdicts(received);
        listeners.forEach(Runnable::run);
    }

    private static EraVerdicts newVerdicts(EraState source)
    {
        return new EraVerdicts(source.eras(), type ->
        {
            ServicePeriod override = source.overrides().get(type.getShortName().toLowerCase(Locale.ROOT));
            return override != null ? override : type.getServicePeriod();
        });
    }
}
