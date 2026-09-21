package com.flansmodultimate.apocalyse.common.util;

import com.flansmodultimate.apocalyse.common.entity.AiMechaEntity;
import com.flansmodultimate.apocalyse.common.entity.FlyByPlaneEntity;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumMechaSlotType;
import com.flansmodultimate.common.driveables.EnumPlaneMode;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.types.PlaneType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Builds the driveables the apocalypse populates itself with. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseDriveableHelper
{
    /** How far out a flyover appears, and the altitude it cruises at. */
    private static final double FLYBY_SPAWN_DISTANCE = 200.0D;
    private static final double FLYBY_ALTITUDE = 120.0D;
    private static final double RADIANS_TO_DEGREES = 180D / Math.PI;

    /**
     * Sends an aircraft across the sky towards {@code target}, flown by a skeleton.
     *
     * @return the aircraft, or empty when no installed pack supplies a plane
     */
    public static Optional<FlyByPlaneEntity> spawnFlyBy(ServerLevel level, Vec3 target, RandomSource random)
    {
        Optional<PlaneType> planeType = randomType(PlaneType.class, random);
        if (planeType.isEmpty())
            return Optional.empty();

        double angle = random.nextDouble() * Math.PI * 2D;
        double offsetX = Math.cos(angle) * FLYBY_SPAWN_DISTANCE;
        double offsetZ = Math.sin(angle) * FLYBY_SPAWN_DISTANCE;
        double altitude = Math.min(FLYBY_ALTITUDE, level.getMaxBuildHeight() - 16D);
        // Point the nose back down the approach so the aircraft passes over the target.
        float yaw = (float) (Mth.atan2(-offsetZ, -offsetX) * RADIANS_TO_DEGREES);

        FlyByPlaneEntity plane = new FlyByPlaneEntity(level, planeType.get(),
            target.x + offsetX, altitude, target.z + offsetZ, yaw);
        // The aircraft boards its own crew once its seats exist, on its first tick.
        return level.addFreshEntity(plane) ? Optional.of(plane) : Optional.empty();
    }

    /**
     * Stands an armed, self-operating mecha at {@code pos} to guard whatever is there.
     *
     * @return the guard, or empty when no installed pack supplies a mecha
     */
    public static Optional<AiMechaEntity> spawnGuardMecha(ServerLevel level, BlockPos pos, RandomSource random)
    {
        Optional<MechaType> mechaType = randomType(MechaType.class, random);
        if (mechaType.isEmpty())
            return Optional.empty();

        MechaType type = mechaType.get();
        AiMechaEntity mecha = new AiMechaEntity(level, type,
            pos.getX() + 0.5D, pos.getY() + type.getYOffset(), pos.getZ() + 0.5D, random.nextFloat() * 360F);
        arm(mecha, type, random);
        return level.addFreshEntity(mecha) ? Optional.of(mecha) : Optional.empty();
    }

    /**
     * Stands a lab guard: the 1.12.2 research lab only drew mechas short enough (3 blocks or less)
     * to fit its 5-block-high rooms.
     */
    public static Optional<AiMechaEntity> spawnDungeonMecha(ServerLevel level, BlockPos pos, RandomSource random)
    {
        List<MechaType> candidates = sortedTypes(MechaType.class).stream()
            .filter(type -> type.getHeight() <= 3F)
            .toList();
        if (candidates.isEmpty())
            return Optional.empty();

        MechaType type = candidates.get(random.nextInt(candidates.size()));
        AiMechaEntity mecha = new AiMechaEntity(level, type,
            pos.getX() + 0.5D, pos.getY() + type.getYOffset(), pos.getZ() + 0.5D, random.nextFloat() * 360F);
        arm(mecha, type, random);
        return level.addFreshEntity(mecha) ? Optional.of(mecha) : Optional.empty();
    }

    /**
     * Parks an aircraft on an airfield, as the 1.12.2 runway did: a random fixed-wing plane
     * with a random compatible engine and every part worn down by a random amount.
     */
    public static Optional<Driveable> spawnParkedPlane(ServerLevel level, double x, double y, double z, RandomSource random)
    {
        List<PlaneType> planes = sortedTypes(PlaneType.class).stream()
            .filter(type -> type.getMode() == EnumPlaneMode.PLANE)
            .toList();
        if (planes.isEmpty())
            return Optional.empty();

        PlaneType type = planes.get(random.nextInt(planes.size()));
        List<PartType> engines = InfoType.getInfoTypes().values().stream()
            .filter(PartType.class::isInstance)
            .map(PartType.class::cast)
            .distinct()
            .filter(part -> part.getCategory() == PartType.Category.ENGINE && !part.isAiChip() && part.worksWith(EnumType.PLANE))
            .sorted(Comparator.comparing(PartType::getShortName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        return Driveable.spawn(level, type, x, y, z, 0F, null, null).map(plane -> {
            DriveableData data = plane.getDriveableData();
            if (!engines.isEmpty())
                data.setEngineShortName(engines.get(random.nextInt(engines.size())).getShortName());
            // Never destroy a part here: that would trigger combat drops and chained part loss.
            for (DriveablePart part : data.getParts().values())
            {
                float maxHealth = part.getMaxHealth();
                if (maxHealth > 1F)
                    part.damage(random.nextFloat() * (maxHealth - 1F), false);
            }
            data.setChanged();
            return plane;
        });
    }

    /** Puts a loaded gun in each hand and spare magazines in the cargo hold. */
    private static void arm(AiMechaEntity mecha, MechaType type, RandomSource random)
    {
        DriveableData data = mecha.getDriveableData();
        if (data == null)
            return;
        int cargoSlots = Math.max(0, type.getNumCargoSlots());

        for (EnumMechaSlotType hand : List.of(EnumMechaSlotType.LEFT_TOOL, EnumMechaSlotType.RIGHT_TOOL))
        {
            Optional<ItemStack> gun = ApocalypseGunHelper.randomLoadedGun(random, false);
            if (gun.isEmpty())
                continue;
            ItemStack gunStack = gun.get();
            data.setMechaAddon(hand, gunStack);
            if (cargoSlots <= 0 || !(gunStack.getItem() instanceof GunItem gunItem))
                continue;

            int magazines = 1 + random.nextInt(2);
            for (int magazine = 0; magazine < magazines; magazine++)
            {
                int slot = random.nextInt(cargoSlots);
                if (!data.getCargo(slot).isEmpty())
                    continue;
                ApocalypseGunHelper.spareAmmoFor(gunItem.getConfigType(), random)
                    .ifPresent(ammo -> data.setCargo(slot, ammo));
            }
        }
        data.setChanged();
    }

    /**
     * A driveable type of the requested kind, chosen reproducibly.
     *
     * <p>Info types live in a hash map, so the candidates are sorted before the worldgen RNG
     * picks one; that keeps a given seed and pack set producing the same choice.</p>
     */
    private static <T extends DriveableType> Optional<T> randomType(Class<T> kind, RandomSource random)
    {
        List<T> candidates = sortedTypes(kind);
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    private static <T extends DriveableType> List<T> sortedTypes(Class<T> kind)
    {
        return InfoType.getInfoTypes().values().stream()
            .filter(kind::isInstance)
            .map(kind::cast)
            .distinct()
            .sorted(Comparator.comparing(DriveableType::getShortName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }
}
