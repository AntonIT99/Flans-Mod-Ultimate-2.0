package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.driveables.*;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.parts.IFlanPartTarget;
import com.flansmodultimate.api.parts.PartArmor;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Physical vehicles and NPC vehicle models viewed through the public API, including an attacker's occupied seat. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcVehicleThreats
{
    public static Entity platform(Entity entity)
    {
        if (entity.getVehicle() instanceof ISeat seat && seat.getDriveable() != null)
            return seat.getDriveable();
        if (entity instanceof ISeat seat && seat.getDriveable() != null)
            return seat.getDriveable();
        return entity.getRootVehicle();
    }

    public static Optional<IDriveableType> type(Entity entity)
    {
        Entity vehicle = platform(entity);
        FlanModelEntity model = NpcModels.flan(vehicle);
        return model != null && model.getInfoType() instanceof IDriveableType type ? Optional.of(type) : FlansEntityTypes.getDriveableType(vehicle);
    }

    public static boolean groundVehicle(Entity entity)
    {
        return type(entity).map(type -> type.getDriveableKind() != DriveableKind.PLANE || type.isShip()).orElse(false);
    }

    public static Vec3 front(Entity entity)
    {
        Entity vehicle = platform(entity);
        if (vehicle instanceof noppes.npcs.entity.EntityNPCInterface npc)
            return Vec3.directionFromRotation(0F, NpcVehicleState.of(npc).getHullYaw());
        return FlansEntityTypes.getAttitude(vehicle).map(DriveableAttitude::front).orElseGet(vehicle::getLookAngle).multiply(1D, 0D, 1D).normalize();
    }

    /** Nominal hull protection from the observer's side. Missing plate data stays unknown. */
    public static Optional<PartArmor> protection(Entity entity, Vec3 observer)
    {
        return protection(entity, observer, "core");
    }

    /** Nominal protection of a chosen part, respecting live NPC overrides and the turret's own facing. */
    public static Optional<PartArmor> protection(Entity entity, Vec3 observer, String part)
    {
        Entity vehicle = platform(entity);
        // A directly targeted occupant can be exposed; hull protection alone cannot establish that the occupant is safe.
        if (vehicle != entity)
            return Optional.empty();
        if (vehicle instanceof noppes.npcs.entity.EntityNPCInterface npc && !NpcEquipment.enabled(npc, Feature.MODEL_ARMOUR))
            return Optional.of(new PartArmor(0F, 0F));
        if (vehicle instanceof IFlanPartTarget target)
        {
            Optional<PartArmor> override = target.flansmodultimatePartBody().flatMap(body -> body.getArmor(part));
            if (override.isPresent())
                return override;
        }
        Vec3 toward = observer.subtract(vehicle.position());
        Vec3 front = front(vehicle);
        if (part.equals("turret"))
        {
            double yaw = vehicle instanceof noppes.npcs.entity.EntityNPCInterface npc ? NpcVehicleState.of(npc).turretAim().yaw() : FlansEntityTypes.getTurretYaw(vehicle).orElse(0D);
            front = front.yRot((float) -Math.toRadians(yaw));
        }
        Vec3 normal = new Vec3(toward.dot(front.cross(new Vec3(0D, 1D, 0D))), toward.y, -toward.dot(front));
        return type(vehicle).flatMap(type -> type.getPartArmor(part, normal));
    }

    /** A visible source used for cover tests, rather than a living passenger's displaced eyes. */
    public static Vec3 fireOrigin(Entity entity)
    {
        Entity vehicle = platform(entity);
        if (vehicle instanceof noppes.npcs.entity.EntityNPCInterface npc && groundVehicle(npc))
            return NpcVehicleState.of(npc).fireOrigin().orElse(npc.getEyePosition());
        return entity.getVehicle() instanceof ISeat seat ? seat.getGunOrigin().orElse(entity.getEyePosition()) : entity.getEyePosition();
    }

    /** Ground footprint of the authored core, since a machine's navigation box may be much narrower than its hull. */
    public static AABB hull(Entity entity)
    {
        Entity vehicle = platform(entity);
        IDriveableType type = type(vehicle).orElse(null);
        if (type == null || type.getCoreBounds().isEmpty())
            return vehicle.getBoundingBox();
        AABB local = type.getCoreBounds().orElseThrow();
        double scale = Math.max(1.0E-4D, type.getModelScale());
        if (vehicle instanceof noppes.npcs.entity.EntityNPCInterface npc)
            scale *= NpcModels.scale(npc);
        Vec3 front = front(vehicle);
        Vec3 right = front.cross(new Vec3(0D, 1D, 0D));
        AABB result = vehicle.getBoundingBox();
        for (double x : new double[]{local.minX, local.maxX})
            for (double z : new double[]{local.minZ, local.maxZ})
            {
                Vec3 point = vehicle.position().add(right.scale(x * scale)).add(front.scale(-z * scale));
                result = result.minmax(new AABB(point.x, vehicle.getY() + local.minY * scale, point.z, point.x, vehicle.getY() + local.maxY * scale, point.z));
            }
        return result;
    }
}
