package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.parts.*;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Per-NPC repair sessions and clocks; all rates operate in model part HP, never Custom NPCs' entity HP. Server thread. */
public final class NpcRepairs
{
    private final Map<String, Float> previous = new HashMap<>();
    private final Map<String, Long> damaged = new HashMap<>();
    private final Map<String, Integer> clocks = new HashMap<>();
    private int remaining;
    private int buoyancyClock;
    private Vec3 navalVelocity = Vec3.ZERO;

    public static boolean active(EntityNPCInterface npc)
    {
        if (!(npc instanceof NpcVehicleStateHolder holder))
            return false;
        NpcVehicleState state = holder.wolffsmodnpcsVehicleState();
        return state != null && state.getParts().repairs().remaining > 0;
    }

    public void damaged(String part, long tick)
    {
        damaged.put(part, tick);
        clocks.remove(part);
    }

    public void reset()
    {
        previous.clear();
        damaged.clear();
        clocks.clear();
        remaining = 0;
        buoyancyClock = 0;
        navalVelocity = Vec3.ZERO;
    }

    /** Applied after normal floating so flooding can lower a ship instead of the waterline controller cancelling it. */
    public void shipMotion(EntityNPCInterface npc, IPartBody body)
    {
        if (npc.level().isClientSide || !npc.isAlive() || !FlansRepairs.properties(body.getType()).map(RepairProperties::ship).orElse(false) || !NpcEquipment.enabled(npc, Feature.PART_EFFECTS))
            return;
        int depth = 0;
        while (depth <= 16 && npc.level().getFluidState(npc.blockPosition().above(depth)).is(net.minecraft.tags.FluidTags.WATER))
            ++depth;
        navalVelocity = FlansRepairs.navalVelocity(body, navalVelocity, npc.getDeltaMovement(), depth);
        npc.setDeltaMovement(navalVelocity);
        if (FlansRepairs.sunk(body, depth))
            npc.setHealth(0F);
    }

    public boolean start(EntityNPCInterface npc, IPartBody body)
    {
        RepairProperties settings = FlansRepairs.properties(body.getType()).orElse(null);
        FlanModelKind kind = FlanModelKind.of(body.getType());
        if (unavailable(npc) || !NpcEquipment.enabled(npc, Feature.AUTOMATIC_REPAIRS) || settings == null || !settings.enabled() || remaining > 0 || kind == FlanModelKind.MECHA || !stationary(npc)
            || kind == FlanModelKind.PLANE && !settings.ship() && !npc.onGround() && npc.getVehicle() == null)
            return false;
        remaining = settings.sessionTicks();
        hold(npc);
        return true;
    }

    public void tick(EntityNPCInterface npc, IPartBody body)
    {
        if (unavailable(npc))
        {
            remaining = 0;
            return;
        }
        if (!NpcEquipment.enabled(npc, Feature.AUTOMATIC_REPAIRS))
        {
            remaining = 0;
            return;
        }
        if (CustomNpcsPlatform.frozen())
            return;
        RepairProperties settings = FlansRepairs.properties(body.getType()).orElse(null);
        if (settings == null)
            return;
        observe(body, npc.level().getGameTime());
        if (remaining == 0 && settings.enabled() && npc.getTarget() == null && body.getHealth() < body.getMaxHealth() * 0.75F && ready(npc))
            start(npc, body);
        boolean carrier = !settings.ship() && FlanModelKind.of(body.getType()) == FlanModelKind.PLANE && stationary(npc) && carrier(npc);
        float crew = settings.ship() ? crew(npc, body, null) : 0F;
        float before = body.getHealth();
        for (PartState part : body.getParts())
            tickPart(npc, body, part, settings, carrier, crew);
        tickBuoyancy(npc, body, settings, crew);
        tickNanobots(npc, body);
        if (body.getHealth() > before)
            changed(npc, body);
        if (remaining > 0)
        {
            hold(npc);
            --remaining;
        }
    }

    private void observe(IPartBody body, long now)
    {
        for (PartState part : body.getParts())
        {
            Float old = previous.put(part.name(), part.health());
            if (old != null && part.health() < old)
            {
                damaged.put(part.name(), now);
                clocks.remove(part.name());
            }
        }
    }

    private boolean ready(EntityNPCInterface npc)
    {
        long now = npc.level().getGameTime();
        return damaged.values().stream().noneMatch(tick -> now - tick < 100L);
    }

    private void tickPart(EntityNPCInterface npc, IPartBody body, PartState part, RepairProperties settings, boolean carrier, float totalCrew)
    {
        if (!part.enabled() || part.destroyed() || part.health() >= part.box().health() || npc.level().getGameTime() - damaged.getOrDefault(part.name(), -100L) < 100L)
        {
            clocks.remove(part.name());
            return;
        }
        if (settings.ship() && part.name().equalsIgnoreCase("buoyancy") || remaining == 0 && !carrier)
            return;
        int interval = settings.ship() ? FlansRepairs.shipInterval(crew(npc, body, part.name()), totalCrew) : 20;
        int clock = clocks.getOrDefault(part.name(), 0) + 1;
        if (clock >= interval)
        {
            float amount = carrier ? FlansRepairs.carrierAmount(part.box().health()) : FlansRepairs.fieldAmount(part.name(), part.box().health(), settings.ship());
            FlansRepairs.repairPart(body, part.name(), amount, false);
            clock = 0;
        }
        clocks.put(part.name(), clock);
    }

    private void tickBuoyancy(EntityNPCInterface npc, IPartBody body, RepairProperties settings, float crew)
    {
        if (!settings.ship())
            return;
        PartState part = body.getPart("buoyancy").orElse(null);
        if (part == null || npc.level().getGameTime() - damaged.getOrDefault(part.name(), -100L) < 100L)
        {
            buoyancyClock = 0;
            return;
        }
        if (++buoyancyClock >= FlansRepairs.buoyancyInterval(crew))
        {
            buoyancyClock = 0;
            FlansRepairs.repairPart(body, part.name(), FlansRepairs.buoyancyAmount(part.box().health()), NpcEquipment.enabled(npc, Feature.PART_REPAIR));
        }
    }

    private static void tickNanobots(EntityNPCInterface npc, IPartBody body)
    {
        if (npc.tickCount % 20 != 0 || FlanModelKind.of(body.getType()) != FlanModelKind.MECHA)
            return;
        List<ItemStack> supplies = NpcAmmunition.supplies(npc);
        float amount = supplies.stream().map(FlansRepairs::nanorepairAmount).max(Float::compare).orElse(0F);
        if (amount <= 0F)
            return;
        for (PartState part : body.getParts())
        {
            if (!part.enabled() || part.destroyed() || part.health() >= part.box().health() || !FlansRepairs.canRepair(body, part.name()))
                continue;
            if (supplies.stream().anyMatch(stack -> FlansRepairs.consumeFuel(stack, 10)))
            {
                FlansRepairs.repairPart(body, part.name(), amount, false);
                npc.updateClient = true;
            }
        }
    }

    public boolean tool(EntityNPCInterface npc, IPartBody body, Vec3 origin, int amount)
    {
        if (unavailable(npc) || !FlansRepairs.repairBody(body, origin, amount, NpcEquipment.enabled(npc, Feature.PART_REPAIR)))
            return false;
        changed(npc, body);
        return true;
    }

    public boolean restore(EntityNPCInterface npc, IPartBody body)
    {
        if (npc.level().isClientSide || !npc.isAlive() || npc.isKilled())
            return false;
        body.restore();
        reset();
        changed(npc, body);
        return true;
    }

    public boolean rebuild(EntityNPCInterface npc, IPartBody body, Player player, String part)
    {
        if (unavailable(npc) || !NpcEquipment.enabled(npc, Feature.PART_REPAIR) || player.level() != npc.level() || !FlansRepairs.withinReach(npc, player.getEyePosition(), 8D)
            || !FlansRepairs.rebuild(body, player, part))
            return false;
        changed(npc, body);
        return true;
    }

    private static boolean unavailable(EntityNPCInterface npc)
    {
        return npc.level().isClientSide || !npc.isAlive() || npc.isKilled() || !NpcEquipment.enabled(npc, Feature.PART_HEALTH);
    }

    private static void changed(EntityNPCInterface npc, IPartBody body)
    {
        if (body.getMaxHealth() > 0F)
            npc.setHealth(npc.getMaxHealth() * body.getHealth() / body.getMaxHealth());
        npc.updateClient = true;
    }

    private static boolean stationary(EntityNPCInterface npc)
    {
        return npc.getDeltaMovement().horizontalDistanceSqr() < 0.0025D;
    }

    public static void hold(EntityNPCInterface npc)
    {
        npc.getNavigation().stop();
        npc.setSpeed(0F);
        npc.setDeltaMovement(0D, npc.getDeltaMovement().y, 0D);
        npc.setXxa(0F);
        npc.setZza(0F);
    }

    private static float crew(EntityNPCInterface npc, IPartBody body, String part)
    {
        int capacity = 0;
        int occupied = 0;
        for (NpcCrew.Seat seat : NpcCrew.seats(npc))
        {
            String compartment = FlansRepairs.seatPart(body.getType(), seat.mount().seat()).orElse("core");
            if (part != null && !part.equalsIgnoreCase(compartment))
                continue;
            ++capacity;
            boolean intact = body.getPart(compartment).map(state -> !state.destroyed()).orElse(true);
            if (intact && npc.getPassengers().stream()
                .anyMatch(rider -> rider instanceof LivingEntity && rider.isAlive() && NpcCrew.seatOf(npc, rider).map(mount -> mount.seat() == seat.mount().seat()).orElse(false)))
                ++occupied;
        }
        if (capacity > 0)
            return (float) occupied / capacity;
        return part == null ? 0F : crew(npc, body, null);
    }

    private static boolean carrier(EntityNPCInterface npc)
    {
        Entity mount = npc.getVehicle();
        if (mount != null && FlansRepairs.isCarrier(mount))
            return true;
        return !npc.level().getEntities(npc, npc.getBoundingBox().inflate(64D, 8D, 64D), candidate -> FlansRepairs.supports(candidate, npc)).isEmpty();
    }
}
