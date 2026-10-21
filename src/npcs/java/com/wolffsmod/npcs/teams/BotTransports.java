package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansEntityTypes;
import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.ITeam;
import com.wolffsmod.npcs.NpcsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Vehicles that bring soldiers to the front and the wrecks destroyed vehicles leave. A transport takes the squad of its
 * vehicle entry on board when it spawns, heads for an objective as infantry would, and puts the squad down once it gets
 * there: beside it on the ground, or by parachute from the air. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotTransports
{
    /** Blocks beyond its task's radius within which a ground transport unloads. */
    private static final double GROUND_DROP_MARGIN = 16D;
    /** Blocks beyond its task's radius, across the ground, within which an aircraft drops its squad. */
    private static final double AIR_DROP_MARGIN = 32D;
    /** Blocks above the ground an aircraft must be for its squad to jump; lower, it lands them like a truck. */
    private static final double JUMP_HEIGHT = 8D;
    /** Blocks around a ground transport its squad gets out over. */
    private static final double UNLOAD_RADIUS = 6D;
    /** Ticks of slow falling a soldier gets when no parachute tool is loaded. */
    private static final int FALL_TICKS = 30 * 20;
    /** Share of its hit points every part of a wreck keeps. */
    private static final float WRECK_HEALTH = 0.3F;

    /** Takes the squad a vehicle entry carries on board a transport that just spawned. */
    static void load(BotRound round, BotSlot slot, BotSquad squad)
    {
        List<BotSettings.VehicleEntry> vehicles = round.getSettings().team(slot.getTeam()).getVehicles();
        String carry = slot.getEntry() < vehicles.size() ? vehicles.get(slot.getEntry()).carry() : "";
        squad.setCargo(SquadTemplates.get().find(carry).isPresent() ? carry : "");
    }

    /** Puts down the squads of the transports that reached their objective. */
    static void unload(ServerLevel level, BotRound round, long now, RandomGenerator random)
    {
        for (BotSquad squad : List.copyOf(round.getSquads().values()))
        {
            Entity vehicle = squad.getCargo().isEmpty() || squad.isDisbanded() ? null : BotCommander.livingMembers(level, round, squad).stream().findFirst().orElse(null);
            if (vehicle != null && arrived(squad, vehicle))
                putDown(level, round, squad, vehicle, now, random);
        }
    }

    /** Whether a transport came close enough, across the ground, to its task's target to put its squad down. */
    private static boolean arrived(BotSquad squad, Entity vehicle)
    {
        SquadTask task = squad.getTask();
        if (task == null)
            return false;
        double margin = squad.getRole().flies() ? AIR_DROP_MARGIN : GROUND_DROP_MARGIN;
        double dx = vehicle.getX() - task.target().x;
        double dz = vehicle.getZ() - task.target().z;
        return Math.sqrt(dx * dx + dz * dz) <= task.radius() + margin;
    }

    private static void putDown(ServerLevel level, BotRound round, BotSquad transport, Entity vehicle, long now, RandomGenerator random)
    {
        String templateId = transport.getCargo();
        transport.setCargo("");
        Optional<SquadTemplate> template = SquadTemplates.get().find(templateId);
        Optional<ITeam> team = round.team(transport.getTeam());
        if (template.isEmpty() || team.isEmpty())
            return;
        double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, vehicle.getBlockX(), vehicle.getBlockZ());
        boolean jump = vehicle.getY() - ground > JUMP_HEIGHT;
        BotSquad cargo = round.newSquad(transport.getTeam(), BotRole.INFANTRY, BotSlot.Source.CARGO, transport.getEntry(), template.get().id());
        Set<String> offered = new HashSet<>();
        team.get().getClasses().forEach(playerClass -> offered.add(playerClass.getShortName()));
        List<String> roster = template.get().roster();
        for (int i = 0; i < roster.size(); i++)
        {
            BotSlot slot = round.newSlot(transport.getTeam(), BotSlot.Source.CARGO, transport.getEntry());
            slot.setRole(offered.contains(roster.get(i)) ? roster.get(i) : BotPopulation.pickClass(team.get(), round.getSettings().team(transport.getTeam()), random));
            slot.setSquad(cargo.getId());
            cargo.getSlots().add(slot.getId());
            if (!BotPopulation.canAfford(round, slot))
            {
                slot.setRetired(true);
                continue;
            }
            Vec3 at = jump
                ? vehicle.position().add((random.nextDouble() - 0.5D) * 4D, -3D, (random.nextDouble() - 0.5D) * 4D)
                : BotFormation.spot(new Vec3(vehicle.getX(), ground, vehicle.getZ()), i + 1, UNLOAD_RADIUS, List.of());
            TeamsBots.spawnAt(level, round, slot, now, random, at).ifPresent(npc ->
            {
                if (jump)
                    parachute(npc);
            });
        }
        cargo.setTask(transport.getTask());
    }

    /** Opens a parachute over a soldier jumping from an aircraft; without one in the content packs, it floats down. */
    private static void parachute(EntityNPCInterface npc)
    {
        if (!FlansEquipment.deployParachute(npc))
            npc.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FALL_TICKS));
    }

    /** Leaves a damaged, empty driveable where a vehicle bot was destroyed, for players to repair and use. */
    static void leaveWreck(ServerLevel level, BotSlot slot, Vec3 position, float yaw)
    {
        slot.setWreckPending(false);
        FlansModApi.getType(slot.getRole()).ifPresent(type ->
        {
            if (FlansEntityTypes.spawnDriveable(level, type, position, yaw, WRECK_HEALTH).isEmpty())
                NpcsMod.log.debug("No room for the wreck of {} at {}", slot.getRole(), position);
        });
    }
}
