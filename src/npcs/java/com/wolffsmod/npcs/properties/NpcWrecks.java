package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.teams.BotTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;

/**
 * Retained wrecks: the visible body an NPC with Retain wreck leaves when it dies. One that never respawns stays where it
 * fell; one that respawns leaves an inert copy of its body behind as it comes back, and so does one that keeps its corpse
 * (see {@link NpcCorpses}). Hide body excludes both Retain wreck and Keep corpse: switching it on switches them off, and
 * switching either of them on switches Hide body off. A wreck is removed once its lifetime has passed: the
 * {@code driveableWreckLifetime} Flan's driveable wrecks last, or the NPC's own Wreck time. Teams bots follow their round's own wreck rules.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcWrecks
{
    /** CustomNPCs' respawn cycles of NPCs that never come back. */
    private static final int NEVER_RESPAWN = 3;
    private static final int DESPAWN = 4;
    private static final int TICKS_PER_SECOND = 20;
    /** Game time the body became a wreck, kept in the entity's persistent data so a reload does not restart its lifetime. */
    private static final String NBT_SINCE = "wolffsmodnpcs_wreck_since";

    /** @return whether the NPC's own body stays as its wreck after death instead of being removed */
    public static boolean retainsBody(EntityNPCInterface npc)
    {
        return npc.stats.spawnCycle == NEVER_RESPAWN && keeps(npc);
    }

    /**
     * @return ticks a retained wreck of the NPC stays, 0 for as long as nothing removes it. The server default follows Flan's
     *         driveable wrecks, whose lifetime of 0 removes them at once, and so the wreck one tick on.
     */
    public static long lifeTicks(EntityNPCInterface npc)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        if (tuning.get(NpcTuning.Value.WRECK_LIFETIME) == NpcTuning.WreckLifetime.CUSTOM.ordinal())
            return (long) tuning.get(NpcTuning.Value.WRECK_TIME) * TICKS_PER_SECOND;
        return Math.max(1, FlansEntityTypes.getDriveableWreckLifetimeTicks());
    }

    /** Ticks a retained body in place of its death: removes it once its lifetime has passed. Server side. */
    public static void tickRetained(EntityNPCInterface npc)
    {
        if (!(npc.level() instanceof ServerLevel level) || npc.isRemoved())
            return;
        CompoundTag data = npc.getPersistentData();
        long now = level.getGameTime();
        if (!data.contains(NBT_SINCE, Tag.TAG_LONG))
            data.putLong(NBT_SINCE, now);
        long ticks = lifeTicks(npc);
        if (ticks > 0 && now - data.getLong(NBT_SINCE) >= ticks)
        {
            level.broadcastEntityEvent(npc, EntityEvent.POOF);
            npc.remove(Entity.RemovalReason.KILLED);
        }
    }

    public static void setKeepWreck(EntityNPCInterface npc, boolean keep)
    {
        NpcPresentation.of(npc).setKeepWreck(keep);
        if (keep)
            npc.stats.hideKilledBody = false;
    }

    public static void setKeepCorpse(EntityNPCInterface npc, boolean keep)
    {
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(Feature.KEEP_CORPSE, keep);
        if (keep)
            npc.stats.hideKilledBody = false;
    }

    public static void setHideBody(EntityNPCInterface npc, boolean hide)
    {
        npc.stats.hideKilledBody = hide;
        if (hide)
        {
            NpcPresentation.of(npc).setKeepWreck(false);
            ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(Feature.KEEP_CORPSE, false);
        }
    }

    /**
     * Called before Custom NPCs revives the NPC. A respawning NPC leaves its wreck or kept corpse as it comes back: a copy of
     * its dead body that never respawns, stays where the NPC died for the wreck lifetime or corpse time and runs no scripts.
     * A revived NPC is no wreck.
     */
    public static void onReset(EntityNPCInterface npc)
    {
        npc.getPersistentData().remove(NBT_SINCE);
        if (!(npc.level() instanceof ServerLevel level) || !npc.hasDied || npc.stats.spawnCycle == NEVER_RESPAWN || npc.stats.spawnCycle == DESPAWN || !keeps(npc) && !keepsCorpse(npc)
            || BotTag.read(npc).isPresent())
            return;
        CompoundTag tag = npc.saveWithoutId(new CompoundTag());
        if (!(npc.getType().create(level) instanceof EntityNPCInterface wreck))
            return;
        wreck.load(tag);
        wreck.setUUID(Mth.createInsecureUUID(level.random));
        // An unlinked copy, or the linked template would bring its respawning back.
        wreck.linkedName = "";
        wreck.stats.spawnCycle = NEVER_RESPAWN;
        wreck.script.setEnabled(false);
        wreck.setHealth(0F);
        level.addFreshEntity(wreck);
    }

    private static boolean keeps(EntityNPCInterface npc)
    {
        return !npc.stats.hideKilledBody && NpcPresentation.of(npc).isKeepWreck();
    }

    /** Whether the NPC keeps its corpse; vehicles keep a wreck or nothing, as {@link NpcCorpses} has it. */
    private static boolean keepsCorpse(EntityNPCInterface npc)
    {
        return !npc.stats.hideKilledBody && ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.KEEP_CORPSE) && NpcPresentation.vehicleType(npc).isEmpty();
    }
}
