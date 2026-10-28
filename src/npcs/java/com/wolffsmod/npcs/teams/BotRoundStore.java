package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.platform.world.NpcSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

/**
 * Keeps the bots of the round being played with the world, so a server that stops in the middle of a round picks them
 * up again where they were, as {@link BotBattleCodec} stores them. The NPCs themselves are saved with their chunks; the
 * round adopts them again when the same playing resumes. Server thread.
 */
public final class BotRoundStore extends NpcSavedData
{
    private static final String ID = "wolffsmodnpcs_bot_round";

    /** The round being played, written out whenever the world is saved. */
    @Nullable
    private BotBattle round;
    /** A round read from disk and not yet resumed, kept as it is until its playing resumes or another one starts. */
    private CompoundTag pending = new CompoundTag();

    public static BotRoundStore get(MinecraftServer server)
    {
        return NpcSavedData.computeIfAbsent(server.overworld(), ID, BotRoundStore::new, BotRoundStore::load);
    }

    /**
     * Restores the bots a round had before the server stopped, when the stored round is the same playing, and keeps the
     * round from now on. A stored round of another playing is dropped.
     *
     * @param now
     *            the level's game time, from which restored bots get a while to be loaded again
     * @return whether bots were restored
     */
    boolean resume(BotBattle current, long now)
    {
        boolean restored = BotBattleCodec.instance(pending).filter(current.getInstance()::equals).isPresent();
        if (restored)
            BotBattleCodec.restore(current, pending, now);
        pending = new CompoundTag();
        round = current;
        setDirty();
        return restored;
    }

    /** Stops keeping a round, which ended. */
    void forget()
    {
        round = null;
        pending = new CompoundTag();
        setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        BotBattle current = round;
        if (current != null)
            BotBattleCodec.write(current, tag);
        else
            tag.merge(pending);
        return tag;
    }

    // NpcSavedData passes the registries; this store holds no registry data.
    @SuppressWarnings("unused")
    static BotRoundStore load(CompoundTag tag, HolderLookup.Provider registries)
    {
        BotRoundStore store = new BotRoundStore();
        store.pending = tag.copy();
        return store;
    }
}
