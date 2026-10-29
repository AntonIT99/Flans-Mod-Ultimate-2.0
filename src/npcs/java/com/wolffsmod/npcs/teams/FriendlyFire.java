package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.entity.FlansFriendlyFire;
import com.flansmodultimate.api.teams.FlansTeams;
import com.wolffsmod.npcs.NpcsCommonConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Friendly fire between Custom NPCs and their friends: two NPCs of one faction or of allied factions, and an NPC and a
 * player its faction is friendly to. Custom NPCs on its own spares NPCs of one faction and friendly NPCs hit by players,
 * but lets allied factions and NPCs hitting friendly players through; these rules decide all of them alike, both as a
 * friendly fire rule of Flan's Mod Ultimate and inside Custom NPCs' own damage checks.
 *
 * <p>
 * Between two fighters of a running Teams round, its game type decides, by its friendlyfire variable. Any other hit
 * between friends follows the NPC's faction, between two NPCs the faction of the one hit, then the world, then the common
 * configuration. Server thread.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FriendlyFire
{
    /** Which friends a setting covers. */
    @Getter
    public enum Kind
    {
        /** NPCs hitting NPCs of their own or an allied faction. */
        NPCS("npcs", "Npcs"),
        /** NPCs and the players their faction is friendly to hitting each other. */
        PLAYERS("players", "Players");

        /** The word commands use. */
        private final String word;
        /** The key the setting is saved under. */
        private final String key;

        Kind(String word, String key)
        {
            this.word = word;
            this.key = key;
        }
    }

    /** Whether a world or faction turns friendly fire on or off, or leaves it to the level above. */
    @Getter
    public enum Setting
    {
        DEFAULT("default"), ON("true"), OFF("false");

        /** The word commands use. */
        private final String word;

        Setting(String word)
        {
            this.word = word;
        }

        /** @return whether friendly fire is on, asking the fallback for the default */
        public boolean or(BooleanSupplier fallback)
        {
            return this == DEFAULT ? fallback.getAsBoolean() : this == ON;
        }
    }

    /** Registers the rules with Flan's Mod Ultimate, which asks them before any damage lands. */
    public static void register()
    {
        FlansFriendlyFire.addRule(FriendlyFire::spares);
    }

    /** The friendly fire rule: whether a hit between friends is held back. */
    static boolean spares(LivingEntity victim, LivingEntity attacker)
    {
        return !lands(victim, attacker);
    }

    /** Whether a hit lands: always between entities that are no friends, between friends only with friendly fire on. */
    public static boolean lands(LivingEntity victim, LivingEntity attacker)
    {
        if (victim == attacker || victim.level().isClientSide)
            return true;
        Optional<Boolean> round = FlansTeams.mayHarm(victim, attacker);
        if (round.isPresent())
            return round.get();
        Optional<Kind> kind = relation(victim, attacker);
        return kind.isEmpty() || enabled(kind.get(), factionOf(victim, attacker), victim.getServer());
    }

    /** The living entity responsible for some damage: whoever caused it, or the owner of the projectile that dealt it. */
    @Nullable
    public static LivingEntity attacker(DamageSource source)
    {
        Entity cause = source.getEntity();
        if (cause instanceof Projectile projectile)
            cause = projectile.getOwner();
        if (cause == null && source.getDirectEntity() instanceof Projectile projectile)
            cause = projectile.getOwner();
        return cause instanceof LivingEntity living ? living : null;
    }

    /**
     * Whether two entities fight on one side, so that a hit between them is no reason to turn on each other: allies in the
     * running round, or friends by their factions.
     */
    public static boolean areFriends(LivingEntity first, LivingEntity second)
    {
        if (FlansTeams.getTeam(first).isPresent() && FlansTeams.getTeam(second).isPresent())
            return !FlansTeams.areEnemies(first, second);
        return relation(first, second).isPresent();
    }

    /** How the victim and attacker are friends, if they are. */
    static Optional<Kind> relation(LivingEntity victim, LivingEntity attacker)
    {
        if (victim instanceof EntityNPCInterface npc && attacker instanceof EntityNPCInterface other)
            return friends(npc.faction, other.faction) ? Optional.of(Kind.NPCS) : Optional.empty();
        if (victim instanceof EntityNPCInterface npc && attacker instanceof Player player)
            return friendlyTo(npc, player);
        if (victim instanceof Player player && attacker instanceof EntityNPCInterface npc)
            return friendlyTo(npc, player);
        return Optional.empty();
    }

    /** Whether NPCs of the two factions are friends: of one faction, or of allied factions. */
    static boolean friends(@Nullable Faction faction, @Nullable Faction other)
    {
        return faction != null && other != null && (faction.id == other.id || FactionRelations.allied(faction, other));
    }

    /** Whether friendly fire of the kind is on for the faction, as the faction, then the world, then the configuration say. */
    static boolean enabled(Kind kind, @Nullable Faction faction, @Nullable MinecraftServer server)
    {
        Setting own = faction == null ? Setting.DEFAULT : FactionFriendlyFire.of(faction).wolffsmodnpcsFriendlyFire(kind);
        return own.or(() -> worldEnabled(kind, server));
    }

    /** Whether friendly fire of the kind is on for factions without a setting of their own. */
    public static boolean worldEnabled(Kind kind, @Nullable MinecraftServer server)
    {
        Setting world = server == null ? Setting.DEFAULT : FriendlyFireSettings.get(server, kind);
        return world.or(() -> configured(kind));
    }

    /** Whether friendly fire of the kind is on in worlds without a setting of their own. */
    public static boolean configured(Kind kind)
    {
        return kind == Kind.NPCS ? NpcsCommonConfig.npcFriendlyFire() : NpcsCommonConfig.playerFriendlyFire();
    }

    /** Saves the settings that are not left to the level above, each as on or off. */
    public static CompoundTag save(Map<Kind, Setting> settings)
    {
        CompoundTag tag = new CompoundTag();
        settings.forEach((kind, setting) ->
        {
            if (setting != Setting.DEFAULT)
                tag.putBoolean(kind.getKey(), setting == Setting.ON);
        });
        return tag;
    }

    public static void load(CompoundTag tag, Map<Kind, Setting> settings)
    {
        settings.clear();
        for (Kind kind : Kind.values())
        {
            if (tag.contains(kind.getKey(), Tag.TAG_BYTE))
                settings.put(kind, tag.getBoolean(kind.getKey()) ? Setting.ON : Setting.OFF);
        }
    }

    private static Optional<Kind> friendlyTo(EntityNPCInterface npc, Player player)
    {
        return npc.faction != null && npc.faction.isFriendlyToPlayer(player) ? Optional.of(Kind.PLAYERS) : Optional.empty();
    }

    /** The faction whose setting decides a hit: the NPC hit, or else the NPC that hit. */
    @Nullable
    private static Faction factionOf(LivingEntity victim, LivingEntity attacker)
    {
        if (victim instanceof EntityNPCInterface npc)
            return npc.faction;
        return attacker instanceof EntityNPCInterface npc ? npc.faction : null;
    }
}
