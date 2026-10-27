/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.driveables.physics.EpicShipPhysics;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.*;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.util.InventoryHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;

/** Shared repair rules for driveables and entities wearing their parts. Mutating methods require the level's server thread. */
@ApiStatus.Experimental
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansRepairs
{
    private static final String TOOL_TICK = "FlansRepairToolTick";

    /** Reads session and servicing settings, or empty for a non-driveable definition. */
    public static Optional<RepairProperties> properties(IContentType content)
    {
        if (!(content instanceof DriveableType type))
            return Optional.empty();
        int duration = type.isEpicShip() ? type.getShipRepairTime() : type.getRepairTime();
        return Optional.of(new RepairProperties(type.isCanRepair(), duration, type.isEpicShip(), type.isCanMountEntity()));
    }

    /** The compartment of a configured seat, for attributing real crew to local repair rates. Either side. */
    public static Optional<String> seatPart(IContentType content, int seat)
    {
        if (!(content instanceof DriveableType type))
            return Optional.empty();
        SeatInfo info = type.getSeat(seat);
        return info == null ? Optional.empty() : Optional.of(info.getPart().getShortName());
    }

    /** HP restored by a field-repair pulse; part names use the definition's convention. */
    public static float fieldAmount(String part, float maximum, boolean ship)
    {
        EnumDriveablePart type = EnumDriveablePart.getPart(part);
        return type == null ? 0F : DriveableRepairRules.timedAmount(type, maximum, ship);
    }

    /** Interval in ticks for naval repairs; crew fractions are bounded to [0,1]. */
    public static int shipInterval(float local, float total)
    {
        return DriveableRepairRules.shipInterval(local, total);
    }

    /** Interval in ticks for buoyancy recovery; crew availability is bounded to [0,1]. */
    public static int buoyancyInterval(float crew)
    {
        return DriveableRepairRules.buoyancyInterval(crew);
    }

    /** HP restored by one second of carrier servicing, scaled to the part's maximum definition HP. */
    public static float carrierAmount(float maximum)
    {
        return DriveableRepairRules.carrierAmount(maximum);
    }

    /** HP restored by one buoyancy recovery pulse, scaled to the part's maximum definition HP. */
    public static float buoyancyAmount(float maximum)
    {
        return DriveableRepairRules.buoyancyAmount(maximum);
    }

    /** Whether a part is damaged, enabled and has intact parent parts; destroyed parts can qualify. Either side. */
    public static boolean canRepair(IPartBody body, String part)
    {
        return body instanceof PartBody parts && parts.canRepair(part);
    }

    /** Repairs a named part in definition HP, optionally rebuilding it. Returns HP gained; does not update the wearer's entity HP. */
    public static float repairPart(IPartBody body, String part, float amount, boolean rebuild)
    {
        return body instanceof PartBody parts ? parts.repair(part, amount, rebuild) : 0F;
    }

    /** Finds a tool-repairable part nearest an origin in world blocks, with the 5% per-part cap. Server thread. */
    public static boolean repairBody(IPartBody body, Vec3 origin, int amount)
    {
        return repairBody(body, origin, amount, true);
    }

    /** Bounded tool repair in definition HP with an explicit policy for rebuilding destroyed parts. Server thread. */
    public static boolean repairBody(IPartBody body, Vec3 origin, int amount, boolean rebuild)
    {
        if (!(body instanceof PartBody parts) || amount <= 0)
            return false;
        return parts.nearestRepairPart(origin, rebuild).map(part -> parts.repair(part.name(), DriveableRepairRules.toolAmount(part.box().health(), amount), rebuild) > 0F).orElse(false);
    }

    /** Rebuilds a destroyed part using player-carried materials. Server thread; callers synchronize the wearer's HP afterward. */
    public static boolean rebuild(IPartBody body, Player player, String name)
    {
        if (player.level().isClientSide || !(body instanceof PartBody parts) || !(body.getType() instanceof DriveableType type))
            return false;
        var state = body.getPart(name).orElse(null);
        if (state == null || !state.enabled() || !state.destroyed() || !parts.canRepair(name))
            return false;
        var required = type.getItemsRequired(new DriveablePart(EnumDriveablePart.getPart(name), null), null);
        boolean creative = player.getAbilities().instabuild;
        if (!creative && !InventoryHelper.canConsumeAll(player.getInventory(), required))
            return false;
        if (parts.repair(name, DriveableRepairRules.rebuildHealth(EnumDriveablePart.getPart(name), state.box().health()), true) <= 0F)
            return false;
        InventoryHelper.tryConsumeAll(player.getInventory(), required, creative);
        return true;
    }

    /** Uses a real repair tool on one target, enforcing one-second actor cooldown, reach, tool HP cap and durability. Server thread. */
    public static boolean useTool(LivingEntity actor, Entity target, ItemStack stack)
    {
        if (actor.level().isClientSide || target.level() != actor.level() || !actor.isAlive() || !target.isAlive() || !(stack.getItem() instanceof ToolItem item)
            || !item.getConfigType().isHealDriveables() || !withinReach(target, actor.getEyePosition(), 6D))
            return false;
        ToolType tool = item.getConfigType();
        if (tool.getToolLife() > 0 && stack.getDamageValue() >= tool.getToolLife())
            return false;
        long now = actor.level().getGameTime();
        if (actor.getPersistentData().contains(TOOL_TICK) && now - actor.getPersistentData().getLong(TOOL_TICK) < 20L)
            return false;
        boolean changed = applyTool(actor, target, tool.getHealAmount());
        if (!changed)
            return false;
        actor.getPersistentData().putLong(TOOL_TICK, now);
        boolean creative = actor instanceof Player player && player.getAbilities().instabuild;
        if (!creative && tool.getToolLife() > 0)
        {
            stack.setDamageValue(stack.getDamageValue() + 1);
            if (tool.isDestroyOnEmpty() && stack.getDamageValue() >= tool.getToolLife())
                stack.shrink(1);
        }
        if (actor instanceof Player player)
            player.getCooldowns().addCooldown(item, 20);
        return true;
    }

    private static boolean applyTool(LivingEntity actor, Entity target, int amount)
    {
        if (target instanceof Driveable driveable)
        {
            if (actor instanceof Player player)
                return driveable.repairFromTool(player, amount);
            return driveable.repairNearestPart(actor.getEyePosition(), amount);
        }
        return target instanceof IFlanRepairTarget repairable && repairable.flansmodultimateRepair(actor.getEyePosition(), amount);
    }

    /** Checks reach in world blocks against real repairable part surfaces, including large or scaled NPC models. Either side. */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Public predicate is also available to addon callers.
    public static boolean withinReach(Entity target, Vec3 origin, double distance)
    {
        if (!Double.isFinite(distance) || distance < 0D)
            return false;
        if (target instanceof IFlanRepairTarget repairable)
            return repairable.flansmodultimatePartBody().filter(PartBody.class::isInstance).map(PartBody.class::cast).map(body -> body.repairDistanceSqr(origin) <= distance * distance).orElse(false);
        return target instanceof Driveable && target.getBoundingBox().distanceToSqr(origin) <= distance * distance;
    }

    /** Chooses the first real driveable or part-body surface intersected by a world-space tool ray, including destroyed parts. Either side. */
    public static Optional<Entity> toolTarget(Player player, Vec3 start, Vec3 end)
    {
        return player.level()
            .getEntities(player, new net.minecraft.world.phys.AABB(start, end).inflate(64D), entity -> (entity instanceof IFlanRepairTarget || entity instanceof Driveable) && entity.isAlive())
            .stream().flatMap(entity -> toolHits(entity, start, end.subtract(start))).min(Comparator.comparingDouble(ToolHit::fraction)).map(ToolHit::entity);
    }

    private static java.util.stream.Stream<ToolHit> toolHits(Entity entity, Vec3 origin, Vec3 motion)
    {
        if (entity instanceof Driveable driveable)
            return driveable.attackFromBullet(origin, motion, true).stream().map(hit -> new ToolHit(entity, hit.getIntersectTime()));
        return ((IFlanRepairTarget) entity).flansmodultimatePartBody().stream().filter(PartBody.class::isInstance).map(PartBody.class::cast)
            .flatMap(body -> body.trace(entity, origin, motion, true).stream()).map(hit -> new ToolHit(entity, hit.getIntersectTime()));
    }

    private record ToolHit(Entity entity, float fraction)
    {}

    /** Whether an alive entity is configured to service aircraft mounted on or supported by its hull. Either side. */
    public static boolean isCarrier(Entity entity)
    {
        if (!entity.isAlive())
            return false;
        if (entity instanceof Driveable driveable)
            return driveable.canMountEntity();
        return entity instanceof IFlanRepairTarget target && target.flansmodultimatePartBody().flatMap(body -> properties(body.getType())).map(RepairProperties::carrier).orElse(false);
    }

    /** Whether a carrier hull actually supports an aircraft's feet. Server thread, after the hull is posed. */
    public static boolean supports(Entity carrier, Entity aircraft)
    {
        if (!isCarrier(carrier))
            return false;
        if (carrier instanceof Driveable driveable)
            return driveable.supportsCarrierService(aircraft);
        return carrier instanceof IFlanRepairTarget target && target.flansmodultimatePartBody().filter(body -> properties(body.getType()).map(RepairProperties::carrier).orElse(false))
            .filter(PartBody.class::isInstance).map(PartBody.class::cast).map(body -> body.supports(aircraft)).orElse(false);
    }

    /** HP per pulse offered by a nanorepair upgrade stack, or zero. Either side. */
    public static float nanorepairAmount(ItemStack stack)
    {
        float amount = stack.getItem() instanceof MechaAddonItem addon && addon.getConfigType().isAutoRepair() ? addon.getConfigType().getAutoRepairAmount() : 0F;
        return Float.isFinite(amount) && amount > 0F ? amount : 0F;
    }

    /** Draws a requested number of fuel units from one real Flan fuel-can stack, preserving partial-can damage. Server thread. */
    public static boolean consumeFuel(ItemStack stack, int amount)
    {
        if (amount <= 0 || !(stack.getItem() instanceof PartItem item) || item.getConfigType().getCategory() != PartType.Category.FUEL)
            return false;
        int capacity = item.getConfigType().getFuel();
        if (capacity - stack.getDamageValue() < amount)
            return false;
        stack.setDamageValue(stack.getDamageValue() + amount);
        if (stack.getDamageValue() >= capacity)
            stack.shrink(1);
        return true;
    }

    /** Naval motion in blocks/tick using shared driveable compartment physics. Either side; does not mutate the entity. */
    public static Vec3 navalVelocity(IPartBody body, Vec3 previous, Vec3 healthy, double depth)
    {
        EpicShipPhysics.Damage damage = navalDamage(body);
        float limit = EpicShipPhysics.throttleLimit(damage, true, depth);
        return new Vec3(healthy.x * limit, EpicShipPhysics.verticalVelocity(previous.y, healthy.y, depth > 1D, damage), healthy.z * limit);
    }

    /** Whether shared naval compartment rules consider a ship irrecoverably sunk at the given water depth in blocks. Either side. */
    public static boolean sunk(IPartBody body, double depth)
    {
        return EpicShipPhysics.shouldDestroy(navalDamage(body), depth);
    }

    private static EpicShipPhysics.Damage navalDamage(IPartBody body)
    {
        return EpicShipPhysics.damage(navalPart(body, "buoyancy"), navalPart(body, "bow"), navalPart(body, "stern"), navalSide(body, "left", "port"), navalSide(body, "right", "starboard"),
            navalPart(body, "midsection"));
    }

    private static EpicShipPhysics.PartHealth navalPart(IPartBody body, String name)
    {
        return body.getPart(name).filter(PartState::enabled).map(part -> new EpicShipPhysics.PartHealth(part.health(), part.box().health())).orElse(new EpicShipPhysics.PartHealth(0F, 0F));
    }

    private static EpicShipPhysics.PartHealth navalSide(IPartBody body, String legacy, String naval)
    {
        EpicShipPhysics.PartHealth part = navalPart(body, legacy);
        return part.maximum() > 0F ? part : navalPart(body, naval);
    }
}
