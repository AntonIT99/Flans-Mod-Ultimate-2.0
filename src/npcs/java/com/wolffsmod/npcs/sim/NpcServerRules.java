package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.teams.FlansTeams;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.teams.BotTag;
import com.wolffsmod.npcs.teams.TeamsBots;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * What the settings of the whole server ask of every NPC: the share of its health it has, the search intervals it follows
 * once an operator hands them to every NPC, and whether it uses up its ammunition. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcServerRules
{
    private static final double SAME = 1.0E-6D;
    /** The least health, in hit points, Cinematic mode leaves a player. */
    private static final float SPARED_HEALTH = 1F;
    /** The NPC's own search intervals, which following the server's sets back to 0. */
    private static final Value[] OWN_INTERVALS = {Value.SEARCH_NEAR_INTERVAL, Value.SEARCH_FAR_INTERVAL, Value.SEARCH_BAND_0_INTERVAL, Value.SEARCH_BAND_1_INTERVAL, Value.SEARCH_BAND_2_INTERVAL,
        Value.SEARCH_BAND_3_INTERVAL, Value.SEARCH_BAND_4_INTERVAL, Value.SEARCH_BAND_5_INTERVAL};

    /** Brings the NPC in line with the server. Called now and then from the NPC's tick. */
    public static void refresh(EntityNPCInterface npc, NpcServerSettings server)
    {
        applyHealth(npc, server);
        FlansTeams.applyBotHealthOverride(npc, BotTag.isBot(npc));
        takeServerSearch(npc, server);
    }

    /** Gives every loaded NPC the share of health the server now sets at once, rather than within a second. */
    public static void healthChanged(MinecraftServer server)
    {
        NpcServerSettings settings = NpcServerSettings.get(server);
        for (ServerLevel level : server.getAllLevels())
            for (Entity entity : level.getAllEntities())
                if (entity instanceof EntityNPCInterface npc)
                {
                    applyHealth(npc, settings);
                    FlansTeams.applyBotHealthOverride(npc, BotTag.isBot(npc));
                }
    }

    /**
     * Puts the server's share on the NPC's most health. The share of its health the NPC has left stays the same, so a
     * wounded NPC stays as wounded.
     */
    static void applyHealth(EntityNPCInterface npc, NpcServerSettings server)
    {
        AttributeInstance health = npc.getAttribute(Attributes.MAX_HEALTH);
        if (health == null)
            return;
        double factor = server.get(NpcServerSettings.Setting.HEALTH_PERCENT) / 100D;
        if (Math.abs(NpcPlatform.serverHealthFactor(health) - factor) < SAME)
            return;
        float before = npc.getMaxHealth();
        float fraction = before > 0F ? Math.min(1F, npc.getHealth() / before) : 1F;
        NpcPlatform.setServerHealthFactor(health, factor);
        if (npc.isAlive() && !npc.isKilled())
            npc.setHealth(fraction * npc.getMaxHealth());
    }

    /**
     * Sets the NPC's own search intervals back to 0, so it follows the server's, when an operator has handed them to every
     * NPC since it last took them.
     */
    private static void takeServerSearch(EntityNPCInterface npc, NpcServerSettings server)
    {
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        if (settings.wolffsmodnpcsSearchGeneration() == server.getSearchGeneration())
            return;
        NpcTuning tuning = settings.wolffsmodnpcsTuning();
        for (Value value : OWN_INTERVALS)
            tuning.set(value, 0);
        settings.wolffsmodnpcsSearchGeneration(server.getSearchGeneration());
    }

    /**
     * @param own
     *            whether the NPC's own switch says it uses up its ammunition
     * @return whether it does, once the server's rule is applied; the NPC's own answer off the server thread
     */
    // An NPC built without a level, as in an editor preview, has no server to ask.
    @SuppressWarnings({"ConstantValue", "java:S2589"})
    public static boolean consumesAmmo(EntityNPCInterface npc, boolean own)
    {
        Level level = npc.level();
        NpcServerSettings server = level == null || level.isClientSide ? null : NpcServerSettings.of(npc);
        return consumesAmmo(server == null ? null : server.ammoPolicy(), server == null ? own : TeamsBots.consumesAmmo(npc, own));
    }

    /** @return whether the NPC may attack players on Peaceful difficulty, as the server's setting says; never off the server thread */
    public static boolean peacefulCombat(EntityNPCInterface npc)
    {
        NpcServerSettings server = npc.level().isClientSide ? null : NpcServerSettings.of(npc);
        return server != null && server.peacefulCombat();
    }

    /**
     * @return the damage a player takes from a hit, after armour, as the server's Cinematic mode allows: never enough to kill
     *         them
     */
    public static float cinematicDamage(LivingEntity victim, DamageSource source, float amount)
    {
        return cinematic(victim, source) ? spared(victim.getHealth(), amount) : amount;
    }

    /**
     * Keeps a player alive whom something killed without a hit Cinematic mode could soften, such as damage set straight on
     * their health.
     *
     * @return whether the player was kept alive, and their death is to be cancelled
     */
    public static boolean survivesCinematic(LivingEntity victim, DamageSource source)
    {
        if (!cinematic(victim, source))
            return false;
        victim.setHealth(SPARED_HEALTH);
        return true;
    }

    /** @return the damage that leaves {@link #SPARED_HEALTH} of the health, at most the damage dealt */
    static float spared(float health, float amount)
    {
        return Math.min(amount, Math.max(0F, health - SPARED_HEALTH));
    }

    /**
     * Whether Cinematic mode spares the player from the damage: everything but what nothing stops, as falling out of the
     * world or {@code /kill}, which would otherwise leave them stuck.
     */
    private static boolean cinematic(LivingEntity victim, DamageSource source)
    {
        if (!(victim instanceof ServerPlayer) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return false;
        NpcServerSettings server = NpcServerSettings.of(victim);
        return server != null && server.cinematic();
    }

    static boolean consumesAmmo(@Nullable NpcServerSettings.AmmoPolicy policy, boolean own)
    {
        if (policy == null)
            return own;
        return switch (policy)
        {
            case PER_NPC -> own;
            case NEVER -> false;
            case ALWAYS -> true;
        };
    }
}
