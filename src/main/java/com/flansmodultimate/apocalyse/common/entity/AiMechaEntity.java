package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.EnumMechaSlotType;
import com.flansmodultimate.common.driveables.MechaPhysics;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.config.ModApocalypseConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * A mecha that guards an apocalypse structure with nobody inside it.
 *
 * <p>It reproduces the 1.7.10 guard: acquire the closest player in range, turn the torso
 * onto them, and fire the guns in its hands while the shot is clear, otherwise close the
 * distance. Everything runs through the ordinary mecha control path, so the machine takes
 * damage, loses parts and drops its contents exactly like a piloted one.</p>
 */
public class AiMechaEntity extends Mecha
{
    private static final String NBT_USING_LEFT = "ai_using_left";
    private static final int TARGET_ACQUIRE_INTERVAL = 40;
    private static final double TARGETING_RANGE = 20.0D;
    /** Below this projection onto a movement axis the input is not worth pressing. */
    private static final double MOVEMENT_DEADZONE = 0.35D;
    /** Close enough: stop walking so the guard does not trample its target. */
    private static final double APPROACH_STOP_DISTANCE_SQR = 9.0D;

    @Nullable
    private Entity target;
    private boolean usingLeft;

    public AiMechaEntity(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public AiMechaEntity(Level level, MechaType type, double x, double y, double z, float yaw)
    {
        super(ApocalypseContent.aiMecha.get(), level, type, x, y, z, yaw, null, ItemStack.EMPTY);
    }

    /** The guard commands itself, so its engine runs and its legs work with an empty seat. */
    @Override
    protected boolean isUnderCommand()
    {
        return true;
    }

    @Override
    protected void tickDriveable()
    {
        if (!level().isClientSide)
            think();
        super.tickDriveable();
    }

    private void think()
    {
        MechaType type = getMechaType();
        if (type == null)
            return;

        // 1.7.10 guards never ran out of fuel; keep the tank full rather than
        // special-casing every fuel check along the way.
        setFuel(type.getFuelTankSize());
        // The base tick drops stale control input after a timeout; this input is never stale.
        inputTimeout = 0;

        if (!ModApocalypseConfig.apocalypseMobsEnabled())
        {
            setInputMask(0);
            return;
        }

        if (target != null && !isValidTarget(target))
            target = null;
        if (target == null && (tickCount + getId()) % TARGET_ACQUIRE_INTERVAL == 0)
            target = acquireTarget();
        if (target == null)
        {
            setInputMask(0);
            return;
        }

        Vec3 muzzle = getEyePosition();
        Vec3 aimPoint = target.position().add(0D, target.getBbHeight() * 0.5D, 0D);
        Vec3 delta = aimPoint.subtract(muzzle);
        aimAt(delta);

        if (hasClearShot(muzzle, aimPoint))
        {
            // A hand at a time, swapping now and then, exactly as the original did.
            setInputMask(triggerFor(usingLeft));
            if (random.nextInt(5) == 0)
                usingLeft = !usingLeft;
            return;
        }
        setInputMask(walkToward(delta));
    }

    /**
     * The fire input for one hand this tick.
     *
     * <p>A semi-automatic weapon only fires as the trigger goes down, so the guard has to let
     * it back up in between. Anything that fires while held is simply held, which is also what
     * lets a minigun spin up.</p>
     */
    private int triggerFor(boolean left)
    {
        int input = left ? DriveableInput.PRIMARY_FIRE : DriveableInput.SECONDARY_FIRE;
        return needsTriggerPulse(left) && tickCount % 2 != 0 ? 0 : input;
    }

    private boolean needsTriggerPulse(boolean left)
    {
        if (getDriveableData() == null)
            return false;
        ItemStack held = getDriveableData().getMechaAddon(left ? EnumMechaSlotType.LEFT_TOOL : EnumMechaSlotType.RIGHT_TOOL);
        if (!(held.getItem() instanceof GunItem gunItem))
            return false;
        return gunItem.getConfigType().getFireMode(held) == EnumFireMode.SEMIAUTO;
    }

    /**
     * Turns the torso so that the hand guns point down {@code delta}.
     *
     * <p>The aim is the inverse of the direction the mecha fires along, so the guns hit
     * whatever the torso is facing without this having to know the yaw convention.</p>
     */
    private void aimAt(Vec3 delta)
    {
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 1.0E-4D)
            return;
        float yaw = (float) (Mth.atan2(-delta.x, delta.z) * (180D / Math.PI));
        float pitch = (float) (-Mth.atan2(delta.y, horizontal) * (180D / Math.PI));
        setOrientation(yaw, 0F, 0F);
        setTurretAim(0F, pitch);
    }

    /**
     * Movement input that carries the mecha along {@code delta}.
     *
     * <p>The wanted direction is projected onto the engine's own forward and strafe axes
     * rather than assumed from the torso yaw, so the guard walks where it means to whichever
     * basis the mecha physics use.</p>
     */
    private int walkToward(Vec3 delta)
    {
        Vec3 wanted = new Vec3(delta.x, 0D, delta.z);
        if (wanted.lengthSqr() < APPROACH_STOP_DISTANCE_SQR)
            return 0;
        wanted = wanted.normalize();

        float movementYaw = MechaPhysics.driverMovementYaw(getYaw() + getTurretYaw());
        Vec3 forwardAxis = MechaPhysics.movementIntent(movementYaw, 1F, 0F);
        Vec3 strafeAxis = MechaPhysics.movementIntent(movementYaw, 0F, 1F);

        int mask = 0;
        double forward = wanted.dot(forwardAxis);
        if (forward > MOVEMENT_DEADZONE)
            mask |= DriveableInput.FORWARD;
        else if (forward < -MOVEMENT_DEADZONE)
            mask |= DriveableInput.BACKWARD;

        double strafe = wanted.dot(strafeAxis);
        if (strafe > MOVEMENT_DEADZONE)
            mask |= DriveableInput.RIGHT;
        else if (strafe < -MOVEMENT_DEADZONE)
            mask |= DriveableInput.LEFT;
        return mask;
    }

    @Nullable
    private Entity acquireTarget()
    {
        AABB range = getBoundingBox().inflate(TARGETING_RANGE);
        return level().getEntities(this, range, this::isValidTarget).stream()
            .min(Comparator.comparingDouble(this::distanceToSqr))
            .orElse(null);
    }

    private boolean isValidTarget(Entity candidate)
    {
        return candidate instanceof Player player
            && player.isAlive()
            && !player.isSpectator()
            && !player.getAbilities().instabuild
            && distanceToSqr(player) <= TARGETING_RANGE * TARGETING_RANGE;
    }

    private boolean hasClearShot(Vec3 from, Vec3 to)
    {
        HitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() != HitResult.Type.BLOCK || hit.getLocation().distanceToSqr(from) >= to.distanceToSqr(from);
    }

    /** Nobody climbs into a guard. Creative players keep the usual administrative access. */
    @Override
    public boolean canPlayerAccess(@NotNull Player player)
    {
        return player.getAbilities().instabuild && super.canPlayerAccess(player);
    }

    /**
     * Ignores hand-to-hand attacks from a player standing on the ground, as in 1.7.10, so a
     * guard has to be dealt with using weapons rather than punched apart.
     */
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof Player player
            && player.onGround() && !player.getAbilities().instabuild)
            return false;
        return super.hurt(source, amount);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        usingLeft = tag.getBoolean(NBT_USING_LEFT);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(NBT_USING_LEFT, usingLeft);
    }
}
