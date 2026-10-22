package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.FlanModelNpcs;
import com.wolffsmod.npcs.model.NpcSkins;
import com.wolffsmod.npcs.names.SoldierNames;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.soldier.FlanSoldierLoadout;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomEntities;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * Makes the NPC of a bot slot: a soldier with a Flan class's loadout, made plain or from the team's Custom NPCs clone, or
 * a vehicle, plane, mecha or AA gun wearing its definition's model, or a soldier crewing such a vehicle. Either way the NPC
 * joins the team's faction and the round as a combatant, never respawns by Custom NPCs' own rules, and is marked as a bot. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotFactory
{
    /** Custom NPCs' respawn setting for an NPC that stays dead. */
    private static final int NEVER_RESPAWN = 3;
    /** Custom NPCs' movement setting for an NPC that stands where it is when idle. */
    private static final int STANDING = 0;
    private static final int SHOW_NAME = 0;
    private static final int HIDE_NAME = 1;
    private static final double SPREAD = 1.5D;
    /** Places tried around a spawn point for a soldier to fit. */
    private static final int SPOT_ATTEMPTS = 6;
    private static final double SOLDIER_HALF_WIDTH = 0.3D;
    private static final double SOLDIER_HEIGHT = 1.95D;
    /** Inventory slots of a Custom NPC, which hold spare ammunition. */
    private static final int INVENTORY_SLOTS = 9;
    /** Custom NPCs' best ranged accuracy. */
    private static final int MAX_ACCURACY = 100;

    /** What a bot is made with. */
    public record Order(BotSlot slot, ITeam team, int factionId, BotSettings settings, BotSettings.TeamForce force, UUID instance, Vec3 position, @Nullable String nameList)
    {}

    /** What a bot is to its round. */
    private enum Membership
    {
        SOLDIER, VEHICLE,
        /** A soldier riding in a vehicle bot, which neither fills a place of its own nor counts on capture points. */
        CREW
    }

    /** @return the new soldier, already in the level, or empty when the team offers no class */
    public static Optional<EntityNPCInterface> soldier(ServerLevel level, Order order, RandomGenerator random)
    {
        Vec3 position = freeSpot(level, order.position(), random);
        return soldier(level, order, order.slot().getRole(), position, random, npc ->
        {
            name(npc, order, random);
            join(npc, order, position, Membership.SOLDIER);
        });
    }

    /**
     * Makes a soldier to crew the vehicle bot of an order, as the team's soldiers are made, with a name of its own.
     *
     * @return the new soldier, already in the level, or empty when the team offers no such class
     */
    public static Optional<EntityNPCInterface> crewman(ServerLevel level, Order order, String playerClass, Vec3 position, RandomGenerator random)
    {
        return soldier(level, order, playerClass, position, random, npc ->
        {
            String name = generate(order.nameList(), random);
            if (name.isEmpty())
                name = order.force().usesClone() ? npc.display.getName() : order.team().getName();
            npc.display.setName(name);
            join(npc, order, position, Membership.CREW);
        });
    }

    private static Optional<EntityNPCInterface> soldier(ServerLevel level, Order order, String className, Vec3 position, RandomGenerator random, Consumer<EntityNPCInterface> enlist)
    {
        IPlayerClass playerClass = order.team().getClasses().stream().filter(candidate -> candidate.getShortName().equals(className)).findFirst().map(IPlayerClass.class::cast).orElse(null);
        if (playerClass == null)
            return Optional.empty();
        EntityNPCInterface npc = fromClone(level, order.force(), position).orElse(null);
        boolean added = npc != null;
        if (npc == null)
        {
            EntityCustomNpc plain = new EntityCustomNpc(CustomEntities.entityCustomNpc, level);
            plain.moveTo(position.x, position.y, position.z, random.nextFloat() * 360F, 0F);
            npc = plain;
        }
        FlanSoldierSelection selection = new FlanSoldierSelection(order.team(), playerClass);
        // A clone may keep the weapons and armour its maker gave it instead of the class's loadout.
        if (!added || !order.force().isKeepCloneEquipment())
            FlanSoldierLoadout.apply(npc, selection);
        // Only clients know the class skin and whether it needs the 64x32 model, so each client puts it on.
        if (npc instanceof EntityCustomNpc custom && !order.force().usesClone() && !playerClass.getSkinOverrideVariants().isEmpty())
        {
            selection.write(custom.modelData.extra);
            NpcSkins.markClassSkin(custom);
        }
        enlist.accept(npc);
        train(npc, order.settings().getDifficulty());
        if (!added)
            level.addFreshEntity(npc);
        npc.setHealth(npc.getMaxHealth());
        return Optional.of(npc);
    }

    /** @return the new vehicle NPC, already in the level, or empty when the definition has no NPC model */
    public static Optional<EntityNPCInterface> vehicle(ServerLevel level, Order order, float yaw, RandomGenerator random)
    {
        Optional<FlanModelEntityType> type = FlanModelNpcs.type(order.slot().getRole());
        Optional<EntityCustomNpc> spawned = FlanModelNpcs.spawn(level, order.slot().getRole(), order.position(), yaw);
        if (type.isEmpty() || spawned.isEmpty())
            return Optional.empty();
        EntityCustomNpc npc = spawned.get();
        IContentType definition = type.get().getInfoType();
        BotRole role = BotRole.of(type.get().getKind(), definition);
        NpcTuning tuning = NpcTuning.of(npc);
        switch (role)
        {
            case PLANE -> tuning.set(Value.FLIGHT_TYPE, NpcTuning.Flight.PLANE.ordinal());
            case HELICOPTER -> tuning.set(Value.FLIGHT_TYPE, NpcTuning.Flight.HELICOPTER.ordinal());
            case NAVAL -> tuning.set(Value.TERRAIN, NpcTuning.Terrain.WATERCRAFT.ordinal());
            case ARMOR -> tuning.set(Value.TERRAIN, NpcTuning.Terrain.GROUND.ordinal());
            default -> {
                // AA guns stay where they are placed and need no movement profile.
            }
        }
        Tactic tactic = switch (role)
        {
            case ARMOR -> Tactic.ADVANCE;
            case PLANE -> Tactic.STRAFING_RUN;
            case HELICOPTER -> Tactic.STRAFE;
            case AA -> Tactic.HOLD;
            default -> Tactic.CUSTOM_NPCS;
        };
        tuning.set(Value.TACTIC, tactic.ordinal());
        if (definition != null)
            stockAmmunition(npc, definition);
        String crew = crewName(order, random);
        npc.display.setName(crew.isEmpty() ? npc.display.getName() : crew);
        join(npc, order, order.position(), Membership.VEHICLE);
        train(npc, order.settings().getDifficulty());
        npc.setHealth(npc.getMaxHealth());
        return Optional.of(npc);
    }

    /** Scales how well a bot fights by the round's difficulty: aim, how far it notices enemies, health and rate of fire. */
    static void train(EntityNPCInterface npc, BotSettings.Difficulty difficulty)
    {
        if (difficulty == BotSettings.Difficulty.DEFAULT)
            return;
        DataStats stats = npc.stats;
        DataRanged ranged = stats.ranged;
        ranged.setAccuracy(Math.max(0, Math.min(MAX_ACCURACY, Math.round(ranged.getAccuracy() * difficulty.accuracy()))));
        stats.setAggroRange(Math.max(1, Math.round(stats.getAggroRange() * difficulty.aggro())));
        stats.setMaxHealth(Math.max(1, Math.round(stats.getMaxHealth() * difficulty.health())));
        int fastest = Math.max(1, Math.round(ranged.getDelayMin() * difficulty.delay()));
        ranged.setDelay(fastest, Math.max(fastest, Math.round(ranged.getDelayMax() * difficulty.delay())));
    }

    /**
     * A spot near a position where a soldier fits: a few places around it are tried, then the position itself, which a
     * spawn point keeps clear.
     */
    static Vec3 freeSpot(ServerLevel level, Vec3 base, RandomGenerator random)
    {
        for (int attempt = 0; attempt < SPOT_ATTEMPTS; attempt++)
        {
            Vec3 spot = base.add((random.nextDouble() * 2D - 1D) * SPREAD, 0D, (random.nextDouble() * 2D - 1D) * SPREAD);
            AABB body = new AABB(spot.x - SOLDIER_HALF_WIDTH, spot.y, spot.z - SOLDIER_HALF_WIDTH, spot.x + SOLDIER_HALF_WIDTH, spot.y + SOLDIER_HEIGHT, spot.z + SOLDIER_HALF_WIDTH);
            boolean ground = !level.getBlockState(BlockPos.containing(spot.x, spot.y - 0.5D, spot.z)).getCollisionShape(level, BlockPos.containing(spot.x, spot.y - 0.5D, spot.z)).isEmpty();
            if (level.noCollision(body) && ground)
                return spot;
        }
        return base;
    }

    /**
     * Gives a vehicle the default ammunition of every gun it mounts: the first gun's in its projectile slot, the others' in
     * its inventory, from which it reloads them. The inventory is never dropped.
     */
    private static void stockAmmunition(EntityCustomNpc npc, IContentType definition)
    {
        List<ItemStack> stocks = new ArrayList<>();
        for (WeaponMount mount : FlansEntityTypes.getMounts(definition))
        {
            mount.gun().flatMap(FlansEquipment::createDefaultAmmunition).filter(ammunition -> stocks.stream().noneMatch(stock -> ItemStack.isSameItemSameTags(stock, ammunition)))
                .ifPresent(stocks::add);
        }
        for (int i = 0; i < stocks.size() && i <= INVENTORY_SLOTS; i++)
        {
            if (i == 0)
                npc.inventory.setProjectile(NpcAPI.Instance().getIItemStack(stocks.get(0)));
            else
            {
                npc.inventory.drops.put(i - 1, NpcAPI.Instance().getIItemStack(stocks.get(i)));
                npc.inventory.dropchance.put(i - 1, 0F);
            }
        }
    }

    /** Places the team's clone, when it has one that exists. */
    private static Optional<EntityNPCInterface> fromClone(ServerLevel level, BotSettings.TeamForce force, Vec3 position)
    {
        if (!force.usesClone())
            return Optional.empty();
        try
        {
            IEntity<?> spawned = NpcAPI.Instance().getClones().spawn(position.x, position.y, position.z, force.getCloneTab(), force.getCloneName(), NpcAPI.Instance().getIWorld(level));
            if (spawned != null && spawned.getMCEntity() instanceof EntityNPCInterface npc)
                return Optional.of(npc);
        }
        // Custom NPCs throws for a clone that does not exist; the bot is then made plain.
        catch (RuntimeException e)
        {
            NpcsMod.log.warn("Cannot spawn clone {} of tab {} for a Teams bot: {}", force.getCloneName(), force.getCloneTab(), e.getMessage());
        }
        return Optional.empty();
    }

    private static void name(EntityNPCInterface npc, Order order, RandomGenerator random)
    {
        if (!order.slot().getName().isEmpty())
        {
            npc.display.setName(order.slot().getName());
            return;
        }
        String name = generate(order.nameList(), random);
        if (name.isEmpty())
            name = order.force().usesClone() ? npc.display.getName() : order.team().getName();
        order.slot().setName(name);
        npc.display.setName(name);
    }

    private static String crewName(Order order, RandomGenerator random)
    {
        if (order.slot().getName().isEmpty())
            order.slot().setName(generate(order.nameList(), random));
        return order.slot().getName();
    }

    /** A name from a soldier name list, or empty for a list that does not exist. */
    static String generate(@Nullable String nameList, RandomGenerator random)
    {
        if (nameList == null || nameList.isEmpty())
            return "";
        int index = SoldierNames.ids().indexOf(nameList);
        if (index < 0)
            return "";
        // An eighth of the bots get female names, where the list has them.
        int gender = random.nextInt(8) == 0 ? 1 : 0;
        return SoldierNames.generate(SoldierNames.FIRST_ID + index, gender);
    }

    /** Makes the NPC a bot of the round: its faction, name tag, staying dead, standing when idle, its mark and its team. */
    private static void join(EntityNPCInterface npc, Order order, Vec3 position, Membership membership)
    {
        BotSettings settings = order.settings();
        switch (settings.getNameplates())
        {
            case ALWAYS, ALLIES -> npc.display.setShowName(SHOW_NAME);
            case NEVER -> npc.display.setShowName(HIDE_NAME);
            default -> {
                // Each NPC keeps its own setting.
            }
        }
        npc.setFaction(order.factionId());
        // Custom NPCs only targets the NPCs of factions its faction attacks when this is on: bots seek out enemy bots.
        npc.advanced.attackOtherFactions = true;
        npc.stats.spawnCycle = NEVER_RESPAWN;
        npc.ais.returnToStart = false;
        npc.ais.movementType = STANDING;
        npc.ais.setStartPos(BlockPos.containing(position));
        // Battles away from players carry on, as long as a player is within the NPC's active range.
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(Feature.STAY_ACTIVE, true);
        boolean vehicle = membership == Membership.VEHICLE;
        BotTag.write(npc, new BotTag.Data(order.instance(), order.slot().getId(), order.team().getShortName(), vehicle, settings.getNameplates(), membership == Membership.CREW));
        float weight = switch (membership)
        {
            case SOLDIER -> settings.getInfantryCaptureWeight();
            case VEHICLE -> settings.getVehicleCaptureWeight();
            // The vehicle counts for its crew.
            case CREW -> 0F;
        };
        if (!FlansTeams.assignCombatant(npc, order.team(), weight))
            NpcsMod.log.warn("Teams bot {} could not join team {}", npc.getUUID(), order.team().getShortName());
        npc.updateClient = true;
        npc.updateAI = true;
    }
}
