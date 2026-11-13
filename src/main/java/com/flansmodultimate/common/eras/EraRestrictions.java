package com.flansmodultimate.common.eras;

import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.network.client.eras.PacketEraState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The server's answer to whether a subject may have or use a type. Every enforcement point asks here rather than keeping
 * rules of its own. Scope providers add the restrictions of rounds and battles; the world's server and dimension
 * restrictions are built in. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EraRestrictions
{
    private static final EraScopeProvider WORLD = new WorldScopes();
    private static final List<EraScopeProvider> providers = new CopyOnWriteArrayList<>(List.of(WORLD));
    private static final Set<UUID> bypassing = ConcurrentHashMap.newKeySet();
    private static final AtomicReference<EraVerdicts> verdicts = new AtomicReference<>(newVerdicts());

    /** Adds the restrictions of another kind of scope, such as Teams rounds or battles. */
    public static void addProvider(EraScopeProvider provider)
    {
        providers.add(provider);
    }

    public static EraVerdicts verdicts()
    {
        return verdicts.get();
    }

    /** Every usable restriction on the subject, in provider order: server, dimension, then rounds and battles. */
    public static List<ScopedRestriction> restrictionsFor(Entity subject)
    {
        if (!(subject.level() instanceof ServerLevel))
            return List.of();
        List<ScopedRestriction> restrictions = new ArrayList<>();
        EraVerdicts current = verdicts();
        for (EraScopeProvider provider : providers)
            provider.collect(subject, scoped ->
            {
                if (current.isUsable(scoped.restriction()))
                    restrictions.add(scoped);
            });
        return restrictions;
    }

    /** Every usable restriction on the level itself, for what is placed there before it is an entity. */
    public static List<ScopedRestriction> restrictionsAt(ServerLevel level)
    {
        List<ScopedRestriction> restrictions = new ArrayList<>();
        EraVerdicts current = verdicts();
        for (EraScopeProvider provider : providers)
            provider.collectAt(level, scoped ->
            {
                if (current.isUsable(scoped.restriction()))
                    restrictions.add(scoped);
            });
        return restrictions;
    }

    /** The level's server and dimension restrictions alone, without those of rounds or battles. */
    public static List<ScopedRestriction> worldRestrictionsAt(ServerLevel level)
    {
        List<ScopedRestriction> restrictions = new ArrayList<>();
        EraVerdicts current = verdicts();
        WORLD.collectAt(level, scoped ->
        {
            if (current.isUsable(scoped.restriction()))
                restrictions.add(scoped);
        });
        return restrictions;
    }

    /** Whether the type may be spawned or placed in the level by anything other than a player. */
    public static boolean isAllowedAt(ServerLevel level, InfoType type)
    {
        return verdicts().check(restrictionsAt(level), type).allowed();
    }

    /** Whether the subject may have and use the type; a bypassing operator always may. */
    public static EraVerdicts.Verdict check(Entity subject, InfoType type)
    {
        if (subject instanceof Player player && isBypassing(player))
            return EraVerdicts.Verdict.ALLOWED;
        return verdicts().check(restrictionsFor(subject), type);
    }

    public static boolean isAllowed(Entity subject, InfoType type)
    {
        return check(subject, type).allowed();
    }

    public static boolean isBypassing(Player player)
    {
        return bypassing.contains(player.getUUID());
    }

    public static void setBypassing(ServerPlayer player, boolean bypass)
    {
        if (bypass)
            bypassing.add(player.getUUID());
        else
            bypassing.remove(player.getUUID());
        sync(player);
    }

    /** A bypass lasts for one session only. */
    public static void loggedOut(Player player)
    {
        bypassing.remove(player.getUUID());
    }

    /** Call after the eras or any date override changed: verdicts are rebuilt and every client is told. */
    public static void datesChanged(MinecraftServer server)
    {
        verdicts.set(newVerdicts());
        syncAll(server);
        if (server != null)
            EraEntities.sweep(server);
    }

    /** Call after a restriction was set or cleared. */
    public static void restrictionsChanged(@Nullable MinecraftServer server)
    {
        syncAll(server);
        if (server != null)
            EraEntities.sweep(server);
    }

    public static void syncAll(@Nullable MinecraftServer server)
    {
        if (server == null)
            return;
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            sync(player);
    }

    /** Tells the player's client what it needs to show; sent on login, on a dimension change and whenever any of it changes. */
    public static void sync(ServerPlayer player)
    {
        EraVerdicts current = verdicts();
        PacketEraState.send(player, new EraState(current.getEras(), IssueDates.overrides().getAll(), restrictionsFor(player), isBypassing(player)));
    }

    private static EraVerdicts newVerdicts()
    {
        return new EraVerdicts(IssueDates.eras(), IssueDates::periodOf);
    }

    /** The world's own restrictions: the server-wide one, then the one of the dimension. */
    private static final class WorldScopes implements EraScopeProvider
    {
        @Override
        public void collect(Entity subject, Consumer<ScopedRestriction> restrictions)
        {
            if (subject.level() instanceof ServerLevel level)
                collectAt(level, restrictions);
        }

        @Override
        public void collectAt(ServerLevel level, Consumer<ScopedRestriction> restrictions)
        {
            EraSavedData data = EraSavedData.get(level.getServer());
            data.getServer().ifPresent(restriction -> restrictions.accept(new ScopedRestriction(EraScope.server(), restriction)));
            String dimension = level.dimension().location().toString();
            data.getDimension(dimension).ifPresent(restriction -> restrictions.accept(new ScopedRestriction(EraScope.dimension(dimension), restriction)));
        }
    }
}
