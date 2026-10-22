package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.FlansEntityTypes;
import com.flansmodultimate.api.ISeat;
import com.wolffsmod.npcs.teams.BotTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;

/**
 * NPCs that drive Flan's driveables crew them too. An NPC that follows a player gets into the driveable its owner rides,
 * a gun seat before a passenger seat, and gets out again when its owner does. Any other takes a free gun seat of a
 * driveable an ally of it drives, and gets out once the driveable has gone without a driver for a while. A gunner mans
 * its seat's gun against its target and leaves its own weapons be; a passenger fights on with its own. Teams bots man
 * guns by their own rules. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DriveableCrew
{
    /** Ticks a crew member waits in a driveable nobody drives before it gets out. */
    static final int DRIVERLESS_TICKS = 100;
    /** Fewest blocks at which a gunner engages, whatever its firing range. */
    private static final double MIN_FIRE_RANGE = 48D;
    /** Cosine of the 3 degrees a seat gun may point off its target and still fire. */
    private static final double FIRE_CONE = Math.cos(Math.toRadians(3D));
    /** Every so many ticks the trigger is let go for a tick, so that a semi-automatic gun fires again. */
    private static final int TRIGGER_RELEASE = 30;

    /**
     * @return the seat the NPC crews: the free seat of the driveable its owner rides, a gun seat first, or else, for an NPC
     *         without an owner, the nearest free gun seat of a driveable an ally drives
     */
    static Optional<Entity> findSeat(EntityNPCInterface npc, double range, long now)
    {
        return npc.level().getEntities(npc, npc.getBoundingBox().inflate(range), entity -> entity instanceof ISeat seat && crewable(npc, seat, now)).stream()
            .min(Comparator.<Entity>comparingInt(entity -> ((ISeat) entity).getGunType().isPresent() ? 0 : 1).thenComparingDouble(npc::distanceToSqr));
    }

    /**
     * Whether the NPC may crew the seat: a free seat that is no driver's, of a driveable that is no wreck, that the NPC did
     * not give up on and that no enemy of it rides; one of its owner's driveable, or a gun seat of one an ally drives.
     */
    static boolean crewable(EntityNPCInterface npc, ISeat seat, long now)
    {
        Entity driveable = seat.getDriveable();
        if (seat.isDriverSeat() || seat.getRiddenByEntity() != null || driveable == null || !driveable.isAlive() || FlansEntityTypes.isWreck(driveable) || BotTag.isBot(npc)
            || NpcDriver.of(npc).refuses(driveable.getUUID(), now) || !NpcDrivers.noEnemyAboard(npc, driveable))
            return false;
        if (npc.getOwner() != null)
            return ownerRides(npc, driveable);
        Entity driver = NpcDrivers.driverOf(driveable);
        return seat.getGunType().isPresent() && driver != null && NpcDrivers.ally(npc, driver);
    }

    /** @return the seat of another than the driver the NPC crews, as long as its switch is on or it got in by itself */
    static Optional<ISeat> crewSeat(EntityNPCInterface npc)
    {
        if (!(npc.getVehicle() instanceof ISeat seat) || seat.isDriverSeat() || seat.getDriveable() == null || BotTag.isBot(npc))
            return Optional.empty();
        return NpcDrivers.enabled(npc) || seat.getDriveable().getUUID().equals(NpcDriver.of(npc).getBoarded()) ? Optional.of(seat) : Optional.empty();
    }

    /** Whether the NPC mans a seat gun, and so leaves its own weapons be. */
    static boolean mansGun(EntityNPCInterface npc)
    {
        return crewSeat(npc).map(seat -> seat.getGunType().isPresent()).orElse(false);
    }

    /**
     * Crews for one tick: mans the seat's gun, and keeps track of whether the NPC still has a reason to be aboard.
     *
     * @return whether the NPC stays: its owner still rides along, or the driveable has not gone without a driver too long
     */
    static boolean tick(EntityNPCInterface npc, ISeat seat, Entity driveable, NpcDriver driver)
    {
        boolean follower = npc.getOwner() != null;
        if (follower && !ownerRides(npc, driveable))
            return false;
        int driverless = NpcDrivers.driverOf(driveable) == null && !follower ? driver.getDriverless() + 1 : 0;
        driver.setDriverless(driverless);
        if (driverless > DRIVERLESS_TICKS)
            return false;
        LivingEntity target = npc.getTarget() != null && npc.getTarget().isAlive() ? npc.getTarget() : null;
        driver.crew(seat.getGunType().isPresent() ? "gunner" : "passenger", target == null ? null : target.getBoundingBox().getCenter(), npc.level().getGameTime());
        if (seat.getGunType().isPresent())
            man(npc, seat, target);
        return true;
    }

    /** Aims the seat's gun at the target and fires it once it points there, within firing range and with nothing solid in the way. */
    private static void man(EntityNPCInterface npc, ISeat seat, @Nullable LivingEntity target)
    {
        Optional<Vec3> origin = seat.getGunOrigin();
        if (target == null || origin.isEmpty())
        {
            seat.setAutomatedTriggers(false, false);
            return;
        }
        Vec3 aim = target.getBoundingBox().getCenter();
        seat.aimAt(aim);
        Vec3 line = aim.subtract(origin.get());
        double range = Math.max(MIN_FIRE_RANGE, npc.stats.ranged.getRange());
        boolean aligned = seat.getGunDirection().map(direction -> direction.dot(line.normalize()) >= FIRE_CONE).orElse(false);
        boolean release = npc.level().getGameTime() % TRIGGER_RELEASE == 0;
        boolean fire = !release && aligned && line.lengthSqr() <= range * range
            && npc.level().clip(new ClipContext(origin.get(), aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc)).getType() == HitResult.Type.MISS;
        seat.setAutomatedTriggers(fire, false);
    }

    /** Whether the player the NPC follows rides the driveable. */
    private static boolean ownerRides(EntityNPCInterface npc, Entity driveable)
    {
        LivingEntity owner = npc.getOwner();
        return owner != null && owner.getVehicle() instanceof ISeat seat && seat.getDriveable() == driveable;
    }
}
