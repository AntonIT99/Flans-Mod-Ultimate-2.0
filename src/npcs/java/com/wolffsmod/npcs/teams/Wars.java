package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.wolffsmod.npcs.NpcsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Runs the wars of the world: wakes engagements at the borders players come near, weighs who gains ground in each, hands
 * chunks over, moves engagements on with the front, and lets those nobody is near fall asleep. Nothing moves while a Teams
 * round is played; the engagements rest then, as every skirmish does. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Wars
{
    private static final int INTERVAL = 100;
    private static final int SECONDS = INTERVAL / 20;
    /** Seconds an engagement nobody is near stays before it falls asleep. */
    private static final int IDLE_SECONDS = 60;
    /** Blocks behind its side of the border a side of an engagement comes in. */
    private static final double ZONE_DEPTH = 40D;
    private static final double ZONE_RADIUS = 16D;
    /** Blocks between the middles of two engagements at least. */
    private static final double SPACING = 96D;
    /** Chunks from a taken chunk within which an engagement may move on to another border. */
    private static final int ONWARD = 4;
    /** Ticks a war waits before trying again to open an engagement that could not start. */
    private static final long RETRY_TICKS = 1200L;
    private static final String PREFIX = "wolffsmodnpcs.war.";

    /** When each war may next try to open an engagement, after one could not start. */
    private static final Map<UUID, Long> RETRY = new HashMap<>();

    /** Runs every war; called every server tick. */
    static void tick(MinecraftServer server)
    {
        if (server.getTickCount() % INTERVAL != 0 || FlansTeams.getRunningRound().isPresent())
            return;
        WarStore wars = WarStore.get(server);
        for (War war : List.copyOf(wars.all()))
        {
            if (!war.isRunning())
                continue;
            for (War.Engagement engagement : List.copyOf(war.getEngagements()))
                keep(server, war, engagement);
            open(server, war);
            wars.setDirty();
        }
    }

    /** Forgets what is kept only while the server runs. */
    static void serverStopping()
    {
        RETRY.clear();
    }

    /** Ends every engagement of a war, whose fighting then stops; its territory stays. */
    static int stop(MinecraftServer server, War war)
    {
        int removed = 0;
        SkirmishStore skirmishes = SkirmishStore.get(server);
        for (War.Engagement engagement : war.getEngagements())
        {
            Optional<Skirmish> skirmish = skirmishes.byId(engagement.getSkirmish());
            if (skirmish.isPresent())
                removed += Skirmishes.stop(server, skirmish.get());
        }
        war.getEngagements().clear();
        war.setRunning(false);
        WarStore.get(server).setDirty();
        return removed;
    }

    /** Whether two factions fight each other in a war: both of it, and at war in Custom NPCs. */
    static BiPredicate<Integer, Integer> atWar(War war)
    {
        return (faction, other) -> !faction.equals(other) && war.getFactions().contains(faction) && war.getFactions().contains(other)
            && (BattleFactions.attacks(faction, other) || BattleFactions.attacks(other, faction));
    }

    // --- Engagements ---

    /** Follows an engagement: it falls asleep with nobody near, and moves on with the front when a side takes ground. */
    private static void keep(MinecraftServer server, War war, War.Engagement engagement)
    {
        Optional<Skirmish> found = SkirmishStore.get(server).byId(engagement.getSkirmish());
        if (found.isEmpty() || !found.get().isLive() || found.get().getBattle() == null)
        {
            war.getEngagements().remove(engagement);
            return;
        }
        Skirmish skirmish = found.get();
        ServerLevel level = server.getLevel(skirmish.getDimension());
        if (level == null)
            return;
        if (!playersNear(level, middle(engagement.getBorder()), war.getRange()))
        {
            engagement.setIdle(engagement.getIdle() + SECONDS);
            if (engagement.getIdle() >= IDLE_SECONDS)
            {
                Skirmishes.stop(server, skirmish);
                war.getEngagements().remove(engagement);
            }
            return;
        }
        engagement.setIdle(0);
        WarFront.Border border = engagement.getBorder();
        BotBattle battle = Objects.requireNonNull(skirmish.getBattle());
        int side = WarFront.pressing(presence(server, level, battle, skirmish, 0, border.second()), presence(server, level, battle, skirmish, 1, border.second()),
            presence(server, level, battle, skirmish, 1, border.first()), presence(server, level, battle, skirmish, 0, border.first()));
        if (engagement.advance(side, SECONDS, war.getFlipSeconds()))
            take(server, war, engagement, skirmish, level, side);
    }

    /** Hands the contested chunk to the side that took it and moves the engagement on, or ends it where the front ends. */
    private static void take(MinecraftServer server, War war, War.Engagement engagement, Skirmish skirmish, ServerLevel level, int side)
    {
        WarFront.Border taken = side == 0 ? engagement.getBorder() : engagement.getBorder().reversed();
        Map<Long, Integer> owners = war.territory(level.dimension());
        owners.put(taken.second(), taken.firstFaction());
        ChunkPos chunk = new ChunkPos(taken.second());
        announce(server, level, war, middle(taken), Component.translatable(PREFIX + "taken", Skirmishes.factionName(taken.firstFaction()), Skirmishes.factionName(taken.secondFaction()),
            chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), war.getName()));
        int first = skirmish.getSides().get(0).faction();
        int second = skirmish.getSides().get(1).faction();
        Optional<WarFront.Border> onward = WarFront.onward(owners, atWar(war), taken, ONWARD).map(border -> border.firstFaction() == first ? border : border.reversed())
            .filter(border -> border.firstFaction() == first && border.secondFaction() == second);
        if (onward.isEmpty())
        {
            // The front ends here, or goes on against another faction: a new engagement opens there if players are near.
            Skirmishes.stop(server, skirmish);
            war.getEngagements().remove(engagement);
            return;
        }
        engagement.setBorder(onward.get());
        engagement.setPressing(-1);
        engagement.setProgress(0);
        place(level, skirmish, onward.get());
    }

    /** Opens engagements at the borders players are near, as long as the war has room for more. */
    private static void open(MinecraftServer server, War war)
    {
        long now = server.getTickCount();
        if (war.getEngagements().size() >= war.getMostEngagements() || RETRY.getOrDefault(war.getId(), 0L) > now)
            return;
        Map<ResourceKey<Level>, List<WarFront.Border>> borders = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            ResourceKey<Level> dimension = player.level().dimension();
            if (player.isSpectator() || !war.covers(dimension))
                continue;
            List<WarFront.Border> near = borders.computeIfAbsent(dimension, ignored -> WarFront.borders(war.territory(dimension), atWar(war)));
            Optional<WarFront.Border> free = near.stream().filter(border -> WarFront.distanceSqr(border, player.chunkPosition().x, player.chunkPosition().z) <= Math.pow(war.getRange() / 16D, 2D))
                .filter(border -> free(server, war, dimension, border)).min(Comparator.comparingDouble(border -> WarFront.distanceSqr(border, player.chunkPosition().x, player.chunkPosition().z)));
            if (free.isPresent() && !engage(server, war, player.serverLevel(), free.get()))
            {
                RETRY.put(war.getId(), now + RETRY_TICKS);
                return;
            }
            if (war.getEngagements().size() >= war.getMostEngagements())
                return;
        }
    }

    /** Whether no engagement of the war is fought near a border. */
    private static boolean free(MinecraftServer server, War war, ResourceKey<Level> dimension, WarFront.Border border)
    {
        Vec3 at = middle(border);
        SkirmishStore skirmishes = SkirmishStore.get(server);
        return war.getEngagements().stream().noneMatch(engagement -> skirmishes.byId(engagement.getSkirmish()).map(skirmish -> skirmish.getDimension().equals(dimension)).orElse(false)
            && middle(engagement.getBorder()).distanceTo(at) < SPACING);
    }

    /** @return whether an engagement opened at a border */
    private static boolean engage(MinecraftServer server, War war, ServerLevel level, WarFront.Border border)
    {
        SkirmishStore skirmishes = SkirmishStore.get(server);
        String name;
        do
        {
            war.setCount(war.getCount() + 1);
            name = war.getName() + "-" + war.getCount();
        } while (skirmishes.named(name).isPresent());
        Skirmish skirmish = new Skirmish(UUID.randomUUID(), name, level.dimension());
        skirmish.setWar(war.getId());
        SkirmishComposer.Request request = new SkirmishComposer.Request(war.getInfantry(), Map.of(), Map.of(), Map.of());
        Vec3[] zones = zones(level, border);
        skirmish.getSides().add(new Skirmish.Side(border.firstFaction(), zones[0], ZONE_RADIUS, request, List.of()));
        skirmish.getSides().add(new Skirmish.Side(border.secondFaction(), zones[1], ZONE_RADIUS, request, List.of()));
        skirmish.setIntent(Skirmish.Intent.PUSH);
        skirmish.setEnd(Skirmish.End.NEVER);
        skirmish.setRespawn(true);
        skirmish.setRespawnSeconds(war.getRespawnSeconds());
        skirmish.setTickets(war.getTickets());
        skirmish.setDifficulty(war.getDifficulty());
        skirmishes.add(skirmish);
        Skirmishes.Outcome outcome = Skirmishes.start(server, skirmish);
        if (!outcome.started())
        {
            skirmishes.remove(skirmish);
            NpcsMod.log.warn("War {} could not open an engagement: {}", war.getName(), outcome.messages().isEmpty() ? "" : outcome.messages().get(0).getString());
            return false;
        }
        war.getEngagements().add(new War.Engagement(skirmish.getId(), border));
        return true;
    }

    /** Moves the zones of an engagement's sides to either side of a border, and its front with them. */
    private static void place(ServerLevel level, Skirmish skirmish, WarFront.Border border)
    {
        Vec3[] zones = zones(level, border);
        for (int side = 0; side < 2; side++)
        {
            Skirmish.Side old = skirmish.getSides().get(side);
            skirmish.getSides().set(side, new Skirmish.Side(old.faction(), zones[side], old.radius(), old.request(), old.teams()));
        }
        if (skirmish.getBattle() != null && skirmish.getBattle().getGround() instanceof SkirmishBattleground ground)
            ground.forgetZones();
        skirmish.setFront(null);
        Skirmishes.moveFront(level, skirmish);
    }

    /** Where the two sides of an engagement at a border come in: behind their chunks, facing each other, on the ground. */
    static Vec3[] zones(ServerLevel level, WarFront.Border border)
    {
        Vec3 first = centre(border.first());
        Vec3 second = centre(border.second());
        Vec3 across = second.subtract(first).normalize();
        Vec3 firstZone = first.subtract(across.scale(ZONE_DEPTH));
        Vec3 secondZone = second.add(across.scale(ZONE_DEPTH));
        return new Vec3[]{SkirmishBattleground.ground(level, firstZone.x, firstZone.z), SkirmishBattleground.ground(level, secondZone.x, secondZone.z)};
    }

    // --- Who stands where ---

    /** @return the bots and the players of a side standing in a chunk */
    private static int presence(MinecraftServer server, ServerLevel level, BotBattle battle, Skirmish skirmish, int side, long chunk)
    {
        int present = 0;
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof LivingEntity living && living.isAlive() && battle.sideOf(slot.getTeam()) == side && entity.chunkPosition().toLong() == chunk)
                present++;
        }
        int faction = skirmish.getSides().get(side).faction();
        FactionMembers factions = FactionMembers.get(server);
        for (ServerPlayer player : level.players())
        {
            if (player.isAlive() && !player.isSpectator() && player.chunkPosition().toLong() == chunk && factions.factionOf(player.getUUID()).filter(member -> member == faction).isPresent())
                present++;
        }
        return present;
    }

    private static boolean playersNear(ServerLevel level, Vec3 at, double range)
    {
        return level.players().stream().anyMatch(player -> !player.isSpectator() && player.position().distanceTo(new Vec3(at.x, player.getY(), at.z)) <= range);
    }

    /** Tells the members of the war's factions, and the players near, about ground taken. */
    private static void announce(MinecraftServer server, ServerLevel level, War war, Vec3 at, Component message)
    {
        FactionMembers factions = FactionMembers.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            boolean member = factions.factionOf(player.getUUID()).map(war.getFactions()::contains).orElse(false);
            boolean near = player.level() == level && player.position().distanceTo(new Vec3(at.x, player.getY(), at.z)) <= war.getRange();
            if (member || near)
                player.sendSystemMessage(message);
        }
    }

    /** @return the middle of a border, at sea level */
    static Vec3 middle(WarFront.Border border)
    {
        return centre(border.first()).add(centre(border.second())).scale(0.5D);
    }

    private static Vec3 centre(long chunk)
    {
        ChunkPos pos = new ChunkPos(chunk);
        return new Vec3(pos.getMiddleBlockX() + 0.5D, 64D, pos.getMiddleBlockZ() + 0.5D);
    }

    // --- Territory ---

    /** Gives a faction the chunks within a square around one, or takes them from whoever held them with a null faction. */
    static int claim(War war, ResourceKey<Level> dimension, ChunkPos around, int radius, Integer faction)
    {
        Map<Long, Integer> owners = war.territory(dimension);
        int changed = 0;
        for (int x = around.x - radius; x <= around.x + radius; x++)
        {
            for (int z = around.z - radius; z <= around.z + radius; z++)
            {
                long chunk = ChunkPos.asLong(x, z);
                if (faction == null ? owners.remove(chunk) != null : !faction.equals(owners.put(chunk, faction)))
                    changed++;
            }
        }
        return changed;
    }

    /**
     * Draws the territory around a chunk as lines of text: a letter for each faction of the war in its order, a dot for no
     * one, a star for a chunk an engagement fights over, and an at sign for the middle.
     */
    static List<String> map(MinecraftServer server, War war, ResourceKey<Level> dimension, ChunkPos around, int radius)
    {
        Map<Long, Integer> owners = war.territory(dimension);
        Set<Long> contested = contested(server, war, dimension);
        List<String> lines = new ArrayList<>();
        for (int z = around.z - radius; z <= around.z + radius; z++)
        {
            StringBuilder line = new StringBuilder();
            for (int x = around.x - radius; x <= around.x + radius; x++)
            {
                long chunk = ChunkPos.asLong(x, z);
                if (x == around.x && z == around.z)
                    line.append('@');
                else if (contested.contains(chunk))
                    line.append('*');
                else
                    line.append(mark(war, owners.get(chunk)));
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /** @return the chunks the engagements of a war fight over in a dimension */
    private static Set<Long> contested(MinecraftServer server, War war, ResourceKey<Level> dimension)
    {
        Set<Long> contested = new HashSet<>();
        SkirmishStore skirmishes = SkirmishStore.get(server);
        for (War.Engagement engagement : war.getEngagements())
        {
            if (skirmishes.byId(engagement.getSkirmish()).map(skirmish -> skirmish.getDimension().equals(dimension)).orElse(false))
            {
                contested.add(engagement.getBorder().first());
                contested.add(engagement.getBorder().second());
            }
        }
        return contested;
    }

    /** @return the letter of a faction of the war, by its order in the war, or a dot for no one */
    private static char mark(War war, Integer owner)
    {
        int index = owner == null ? -1 : war.getFactions().indexOf(owner);
        return index < 0 ? '.' : (char) ('A' + index);
    }
}
