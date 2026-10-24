package com.flansmodultimate.common.driveables.parts;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.parts.IPartBody;
import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.api.parts.PartBodyPose;
import com.flansmodultimate.api.parts.PartBox;
import com.flansmodultimate.api.parts.PartState;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.driveables.collision.*;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.PartBodyHit;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.PlaneType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The part boxes and part health of a driveable definition, posed in the world for an entity that is not a driveable.
 *
 * <p>
 * Boxes are kept in the frame type files author them in, which is also the frame a static model preview is drawn in,
 * and are traced through the same hull and turret transforms {@code Driveable} uses, so a stand-in is hit where its
 * model is drawn. The body stays level: only its yaw and its turret turn it.
 * </p>
 */
public final class PartBody implements IPartBody
{
    private static final String NBT_PARTS = "parts";
    private static final String NBT_PART = "part";
    /** How far from its hit a detonation still belongs to the shaped charge that struck, squared, as on a driveable. */
    private static final double SHAPED_CHARGE_MATCH_DISTANCE_SQR = 4D;

    @Getter
    private final IContentType type;
    /** Every part in the order it is listed: the core first. */
    private final Map<EnumDriveablePart, DriveablePart> parts = new LinkedHashMap<>();
    private final Map<EnumDriveablePart, CollisionBox> definition = new EnumMap<>(EnumDriveablePart.class);
    private final Set<EnumDriveablePart> custom = EnumSet.noneOf(EnumDriveablePart.class);
    private final Set<EnumDriveablePart> disabled = EnumSet.noneOf(EnumDriveablePart.class);
    private final Map<EnumDriveablePart, PartArmor> armorOverrides = new EnumMap<>(EnumDriveablePart.class);
    @Nullable
    private ResolvedVehicleArmor resolvedArmor;
    private final Vec3 turretPivot;
    private final Vec3 turretOffset;
    @Getter
    private PartBodyPose pose = PartBodyPose.REST;
    /** World directions of the model-local axes at the current yaw. */
    private Vec3 axisX = Vec3.ZERO;
    private Vec3 axisY = Vec3.ZERO;
    private Vec3 axisZ = Vec3.ZERO;
    private boolean brokenPartsSolid;
    @Getter
    private boolean outlined;
    /** Unscaled distance from the model origin to the farthest box corner, or negative until measured. */
    private double reach = -1D;
    @Nullable
    private ShapedChargeImpact shapedChargeImpact;
    /** The entity wearing the body; null for a body that only holds parts. */
    @Nullable
    private Entity owner;
    @Getter
    private boolean solid;
    /** Created once the body is first made solid. */
    @Nullable
    private PartBodyCollisionHelper collision;

    private record ShapedChargeImpact(EnumDriveablePart part, Vec3 point, long gameTime)
    {}

    /** The eight corners of a part box, relative to the model origin in world axes, and whether rounds stop at it. */
    // Corner arrays are built once per call and handed to one renderer.
    @SuppressWarnings("java:S6218")
    public record Outline(Vec3[] corners, boolean hittable)
    {}

    /**
     * @param turretPivot
     *            the point turret parts turn around, in the model-local frame
     * @param turretOffset
     *            the offset turret parts carry round with them, in the model-local frame
     */
    public PartBody(IContentType type, Map<EnumDriveablePart, CollisionBox> boxes, Vec3 turretPivot, Vec3 turretOffset)
    {
        this.type = type;
        this.turretPivot = turretPivot;
        this.turretOffset = turretOffset;
        CollisionBox core = boxes.get(EnumDriveablePart.CORE);
        if (core != null)
            add(EnumDriveablePart.CORE, core);
        boxes.forEach((part, box) ->
        {
            if (part != EnumDriveablePart.CORE && box != null)
                add(part, box);
        });
        setPose(PartBodyPose.REST);
    }

    public static PartBody of(DriveableType type)
    {
        boolean plane = type instanceof PlaneType;
        return new PartBody(type, type.getHealth(), configuredLocal(LegacyDriveableCoordinates.toLocal(type.getTurretOrigin()), plane),
            configuredLocal(LegacyDriveableCoordinates.toLocal(type.getTurretOriginOffset()), plane));
    }

    private static Vec3 configuredLocal(Vec3 local, boolean plane)
    {
        return plane ? LegacyDriveableCoordinates.applyPlaneModelFacing(local) : local;
    }

    private void add(EnumDriveablePart part, CollisionBox box)
    {
        definition.put(part, box);
        parts.put(part, new DriveablePart(part, box));
    }

    public boolean isEmpty()
    {
        return parts.isEmpty();
    }

    /** Names the entity that wears the body, which its solid boxes then let pass. */
    public void setOwner(Entity owner)
    {
        this.owner = owner;
    }

    // Parts

    @Override
    public List<PartState> getParts()
    {
        List<PartState> states = new ArrayList<>(parts.size());
        parts.values().forEach(part -> states.add(state(part)));
        return Collections.unmodifiableList(states);
    }

    @Override
    public Optional<PartState> getPart(String part)
    {
        return Optional.ofNullable(find(part)).map(this::state);
    }

    private PartState state(DriveablePart part)
    {
        EnumDriveablePart id = part.getType();
        CollisionBox box = Objects.requireNonNull(part.getBox());
        return new PartState(id.getShortName(), id.getName(), toBox(box), toBox(definition.get(id)), custom.contains(id), !disabled.contains(id), EnumDriveablePart.isTurretMounted(id),
            part.getHealth(), part.isDestroyed(), part.isOnFire());
    }

    private static PartBox toBox(CollisionBox box)
    {
        return new PartBox(box.asAabb(), box.getHealth(), box.getPenetrationResistance());
    }

    @Nullable
    private DriveablePart find(@Nullable String name)
    {
        EnumDriveablePart part = EnumDriveablePart.getPart(name);
        return part == null ? null : parts.get(part);
    }

    private boolean enabled(DriveablePart part)
    {
        return !disabled.contains(part.getType()) && part.getBox() != null;
    }

    @Override
    public void setBox(String name, @Nullable PartBox box)
    {
        DriveablePart part = find(name);
        if (part == null)
            return;
        EnumDriveablePart id = part.getType();
        CollisionBox authored = definition.get(id);
        if (box == null)
        {
            custom.remove(id);
            part.updateDebugBox(authored);
        }
        else
        {
            AABB bounds = box.bounds();
            custom.add(id);
            part.updateDebugBox(CollisionBox.inWorldUnits(box.health(), (float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, (float) bounds.getXsize(), (float) bounds.getYsize(),
                (float) bounds.getZsize(), box.penetrationResistance(), authored.getCrewDamageMultiplier()));
        }
        reach = -1D;
    }

    @Override
    public void setEnabled(String name, boolean enabled)
    {
        DriveablePart part = find(name);
        if (part == null)
            return;
        if (enabled)
            disabled.remove(part.getType());
        else
            disabled.add(part.getType());
        reach = -1D;
    }

    @Override
    public void setArmor(String name, @Nullable PartArmor armor)
    {
        DriveablePart part = find(name);
        if (part == null)
            return;
        PartArmor previous = armor == null ? armorOverrides.remove(part.getType()) : armorOverrides.put(part.getType(), armor);
        if (!Objects.equals(previous, armor))
            resolvedArmor = null;
    }

    @Override
    public Optional<PartArmor> getArmor(String name)
    {
        DriveablePart part = find(name);
        return part == null ? Optional.empty() : Optional.ofNullable(armorOverrides.get(part.getType()));
    }

    @Override
    public Optional<PartArmor> getModelArmor(String name)
    {
        DriveablePart part = find(name);
        if (part == null)
            return Optional.empty();
        ArmorPlate plate = type instanceof DriveableType driveable ? driveable.getResolvedArmor().plate(part.getType(), EnumArmorFacing.FRONT).authored() : ArmorPlate.UNARMOURED;
        return Optional.of(new PartArmor(plate.thicknessMm(), plate.heatThicknessMm()));
    }

    /** The model's plates with this body's per-part replacements, shared by rounds, blasts and debug readouts. */
    public ResolvedVehicleArmor armor()
    {
        if (resolvedArmor == null)
        {
            VehicleArmorSpec model = type instanceof DriveableType driveable ? driveable.getResolvedArmor().source() : VehicleArmorSpec.EMPTY;
            Map<EnumDriveablePart, ArmorPlate> overrides = new EnumMap<>(EnumDriveablePart.class);
            overrides.putAll(model.partOverrides());
            armorOverrides.forEach((part, armor) -> overrides.put(part, new ArmorPlate(armor.thicknessMm(), 0F, armor.heatThicknessMm())));
            resolvedArmor = VehicleArmorResolver.resolve(new VehicleArmorSpec(model.hull(), model.turret(), overrides), parts.keySet());
        }
        return resolvedArmor;
    }

    @Override
    public void setBrokenPartsSolid(boolean solid)
    {
        brokenPartsSolid = solid;
    }

    @Override
    public void setOutlined(boolean outlined)
    {
        this.outlined = outlined;
    }

    @Override
    public void setSolid(boolean solid)
    {
        this.solid = solid;
    }

    /** Whether rounds and blasts stop at the part: a broken part has been shot off unless broken parts stay solid. */
    private boolean hittable(DriveablePart part)
    {
        return enabled(part) && (brokenPartsSolid || part.getMaxHealth() <= 0F || !part.isDestroyed());
    }

    public float penetrationResistance(EnumDriveablePart part)
    {
        DriveablePart state = parts.get(part);
        return state == null ? 0F : state.getPenetrationResistance();
    }

    public boolean isIntact(EnumDriveablePart part)
    {
        DriveablePart state = parts.get(part);
        return state != null && enabled(state) && !state.isDestroyed();
    }

    /**
     * Whether the wearer's model draws the part: everything but a part that is broken. A part the body has no box for,
     * or one switched off as a hitbox, is still drawn.
     */
    public boolean isDrawn(@Nullable EnumDriveablePart part)
    {
        DriveablePart state = part == null ? null : parts.get(part);
        return state == null || !enabled(state) || !state.isDestroyed();
    }

    /** @return the state of the given part, or null when the body has no box for it */
    @Nullable
    public DriveablePart partState(@Nullable EnumDriveablePart part)
    {
        return part == null ? null : parts.get(part);
    }

    /** @return the state of every part with a box that is switched on, the core first, as the body holds them */
    public List<DriveablePart> enabledParts()
    {
        List<DriveablePart> enabledParts = new ArrayList<>(parts.size());
        for (DriveablePart part : parts.values())
        {
            if (enabled(part))
                enabledParts.add(part);
        }
        return enabledParts;
    }

    /**
     * World position of a point given as shares of a part box's extent, following the turret as hits do.
     *
     * @param fx
     *            share along the box's width, from 0 to 1, like {@code fy} and {@code fz}
     */
    public Vec3 partPointToWorld(DriveablePart part, double fx, double fy, double fz)
    {
        if (part.getBox() == null)
            return pose.origin();
        AABB box = partBoxModelLocal(part);
        Vec3 local = new Vec3(Mth.lerp(fx, box.minX, box.maxX), Mth.lerp(fy, box.minY, box.maxY), Mth.lerp(fz, box.minZ, box.maxZ));
        return toWorld(DriveableProjectileCollision.partPointToHullLocal(local, part.getType(), pose.turret().yaw(), pose.turret().pitch(), turretPivot, turretOffset));
    }

    // Health

    @Override
    public float getHealth()
    {
        float total = 0F;
        for (DriveablePart part : parts.values())
        {
            if (enabled(part))
                total += part.getHealth();
        }
        return total;
    }

    @Override
    public float getMaxHealth()
    {
        float total = 0F;
        for (DriveablePart part : parts.values())
        {
            if (enabled(part))
                total += part.getMaxHealth();
        }
        return total;
    }

    @Override
    public float damage(String name, float amount, boolean fire)
    {
        DriveablePart part = find(name);
        if (part == null || !enabled(part) || part.isDestroyed() || !Float.isFinite(amount) || amount <= 0F && !fire)
            return 0F;
        float before = getHealth();
        if (part.damage(amount, fire))
            breakChildren(part.getType());
        return before - getHealth();
    }

    /** A broken part takes along what hangs on it, as on a driveable. */
    private void breakChildren(EnumDriveablePart broken)
    {
        for (EnumDriveablePart child : broken.getChildren())
        {
            DriveablePart part = parts.get(child);
            if (part != null && !part.isDestroyed() && part.getMaxHealth() > 0F)
            {
                part.damage(part.getMaxHealth(), false);
                breakChildren(child);
            }
        }
    }

    @Override
    public void setHealth(String name, float health)
    {
        DriveablePart part = find(name);
        if (part != null && Float.isFinite(health))
            part.applyNetworkState(health, part.getFireTime(), part.isOnFire(), false);
    }

    @Override
    public void restore()
    {
        parts.values().forEach(part -> part.applyNetworkState(part.getMaxHealth(), 0, false, false));
    }

    @Override
    public float tickFire()
    {
        float before = Float.NaN;
        for (DriveablePart part : parts.values())
        {
            if (!part.isOnFire() && part.getFireTime() <= 0)
                continue;
            if (Float.isNaN(before))
                before = getHealth();
            boolean intact = !part.isDestroyed();
            part.tick();
            if (intact && part.isDestroyed())
                breakChildren(part.getType());
        }
        return Float.isNaN(before) ? 0F : before - getHealth();
    }

    @Override
    public CompoundTag saveState()
    {
        ListTag list = new ListTag();
        parts.values().forEach(part -> list.add(part.save(new CompoundTag())));
        CompoundTag tag = new CompoundTag();
        tag.put(NBT_PARTS, list);
        return tag;
    }

    @Override
    public void loadState(CompoundTag state)
    {
        ListTag list = state.getList(NBT_PARTS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++)
        {
            CompoundTag tag = list.getCompound(index);
            DriveablePart part = find(tag.getString(NBT_PART));
            if (part != null)
                part.load(tag);
        }
    }

    @Override
    public int signature()
    {
        int signature = (Boolean.hashCode(brokenPartsSolid) * 31 + Boolean.hashCode(outlined)) * 31 + armorOverrides.hashCode();
        for (DriveablePart part : parts.values())
        {
            CollisionBox box = Objects.requireNonNull(part.getBox());
            signature = 31 * signature
                + Arrays.hashCode(new float[]{box.getX(), box.getY(), box.getZ(), box.getWidth(), box.getHeight(), box.getDepth(), box.getHealth(), box.getPenetrationResistance(), part.getHealth()});
            signature = 31 * signature + (part.isDestroyed() ? 1 : 0) + (part.isOnFire() ? 2 : 0) + (disabled.contains(part.getType()) ? 4 : 0);
        }
        return signature;
    }

    // Pose

    @Override
    public void setPose(PartBodyPose pose)
    {
        this.pose = pose == null ? PartBodyPose.REST : pose;
        // The model faces the vanilla yaw a quarter turn on from the yaw driveables simulate with.
        float driveableYaw = this.pose.yaw() - 90F;
        axisX = LegacyDriveableCoordinates.modelLocalToWorldDirection(new Vec3(1D, 0D, 0D), driveableYaw, 0F, 0F);
        axisY = LegacyDriveableCoordinates.modelLocalToWorldDirection(new Vec3(0D, 1D, 0D), driveableYaw, 0F, 0F);
        axisZ = LegacyDriveableCoordinates.modelLocalToWorldDirection(new Vec3(0D, 0D, 1D), driveableYaw, 0F, 0F);
        collide();
    }

    /** Poses the solid boxes with the body, or takes them away once it is no longer solid. */
    private void collide()
    {
        Entity wearer = owner;
        if (!solid || wearer == null)
        {
            if (collision != null)
                collision.unregister();
            return;
        }
        if (collision == null)
            collision = new PartBodyCollisionHelper(wearer, () -> wearer.isAlive() && PartBodies.of(wearer) == this);
        Map<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        int signature = Float.hashCode(pose.scale());
        for (DriveablePart part : parts.values())
        {
            CollisionBox box = part.getBox();
            if (!enabled(part) || box == null)
                continue;
            boxes.put(part.getType(), box);
            signature = 31 * signature + Arrays.hashCode(new float[]{part.getType().ordinal(), box.getX(), box.getY(), box.getZ(), box.getWidth(), box.getHeight(), box.getDepth()});
        }
        collision.tick(boxes, signature, pose.origin(), pose.yaw(), pose.scale(), pose.turret().yaw(), pose.turret().pitch(), turretPivot, turretOffset, part ->
        {
            DriveablePart state = parts.get(part);
            return state != null && hittable(state);
        });
    }

    /** @return blocks from the model origin within which every part box lies, at the current scale */
    public double reach()
    {
        if (reach < 0D)
        {
            double farthest = 0D;
            for (DriveablePart part : parts.values())
            {
                if (!enabled(part))
                    continue;
                AABB box = partBoxModelLocal(part);
                boolean turret = EnumDriveablePart.isTurretMounted(part.getType());
                Vec3 centre = turret ? turretPivot : Vec3.ZERO;
                double corner = 0D;
                for (int index = 0; index < 8; index++)
                    corner = Math.max(corner, corner(box, index).distanceTo(centre));
                farthest = Math.max(farthest, turret ? turretPivot.length() + corner + turretOffset.length() : corner);
            }
            reach = farthest;
        }
        return reach * pose.scale();
    }

    /** The orthogonal inverse of {@link #directionToWorld}, without the scale. */
    private Vec3 directionToLocal(Vec3 world)
    {
        return new Vec3(world.dot(axisX), world.dot(axisY), world.dot(axisZ));
    }

    private Vec3 directionToWorld(Vec3 local)
    {
        return axisX.scale(local.x).add(axisY.scale(local.y)).add(axisZ.scale(local.z));
    }

    private Vec3 toLocal(Vec3 world)
    {
        return directionToLocal(world.subtract(pose.origin())).scale(1D / pose.scale());
    }

    private Vec3 toWorld(Vec3 local)
    {
        return pose.origin().add(directionToWorld(local).scale(pose.scale()));
    }

    /** Type files author boxes in the frame the model is drawn mirrored from; see {@code Driveable#partFrameToModelLocal}. */
    private static Vec3 mirror(Vec3 vector)
    {
        return new Vec3(-vector.x, vector.y, vector.z);
    }

    private static AABB partBoxModelLocal(DriveablePart part)
    {
        AABB bounds = Objects.requireNonNull(part.getBox()).asAabb();
        return new AABB(mirror(new Vec3(bounds.minX, bounds.minY, bounds.minZ)), mirror(new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ)));
    }

    private static Vec3 corner(AABB box, int index)
    {
        return new Vec3((index & 1) == 0 ? box.minX : box.maxX, (index & 2) == 0 ? box.minY : box.maxY, (index & 4) == 0 ? box.minZ : box.maxZ);
    }

    // Hit detection

    /**
     * Traces a round against every part that can be hit.
     *
     * @param owner
     *            the entity wearing the body, which the hits name
     * @param origin
     *            world position the round starts the tick at
     * @param motion
     *            world displacement of the round over the tick
     * @return one hit per part the path crosses, nearest first
     */
    public List<BulletHit> trace(Entity owner, Vec3 origin, Vec3 motion)
    {
        return trace(owner, origin, motion, false);
    }

    /**
     * Traces a ray against the parts, as {@link #trace(Entity, Vec3, Vec3)} does.
     *
     * @param includeBroken
     *            whether broken parts are hit too, so that their boxes can still be pointed at
     */
    public List<BulletHit> trace(Entity owner, Vec3 origin, Vec3 motion, boolean includeBroken)
    {
        if (motion.lengthSqr() < 1.0E-12D)
            return List.of();
        Vec3 localOrigin = toLocal(origin);
        Vec3 localMotion = directionToLocal(motion).scale(1D / pose.scale());
        List<BulletHit> hits = new ArrayList<>();
        for (DriveablePart part : parts.values())
        {
            boolean traced = includeBroken ? enabled(part) : hittable(part);
            DriveableProjectileCollision.LocalHit hit = traced
                ? DriveableProjectileCollision.trace(partBoxModelLocal(part), localOrigin, localMotion, part.getType(), pose.turret().yaw(), pose.turret().pitch(), turretPivot, turretOffset)
                : null;
            if (hit == null)
                continue;
            Vec3 normal = mirror(hit.outwardNormal());
            // Handed back at its world magnitude: armour reads the speed of the round from it.
            hits.add(new PartBodyHit(owner, this, part.getType(), hit.fraction(), origin.add(motion.scale(hit.fraction())), mirror(hit.projectileDirection()).scale(pose.scale()),
                EnumArmorFacing.fromOutwardNormal(normal)));
        }
        hits.sort(Comparator.naturalOrder());
        return hits;
    }

    /** One target per part a blast can damage, nearest first; see {@code Driveable#resolveExplosionTargets}. */
    public List<VehicleExplosionTarget> explosionTargets(Vec3 worldPoint)
    {
        Vec3 hullPoint = toLocal(worldPoint);
        float yaw = pose.turret().yaw();
        float pitch = pose.turret().pitch();
        List<VehicleExplosionTarget> targets = new ArrayList<>();
        for (DriveablePart part : parts.values())
        {
            if (!enabled(part) || !part.canReceiveExplosionDamage())
                continue;
            AABB box = partBoxModelLocal(part);
            DriveableProjectileCollision.ClosestSurface surface = DriveableProjectileCollision.closestSurface(box, hullPoint, part.getType(), yaw, pitch, turretPivot, turretOffset);
            Vec3 worldSurface = toWorld(DriveableProjectileCollision.partPointToHullLocal(surface.position(), part.getType(), yaw, pitch, turretPivot, turretOffset));
            List<Vec3> samples = new ArrayList<>();
            for (Vec3 sample : DriveableProjectileCollision.explosionSurfaceSamples(box, surface))
                samples.add(toWorld(DriveableProjectileCollision.partPointToHullLocal(sample, part.getType(), yaw, pitch, turretPivot, turretOffset)));
            targets.add(new VehicleExplosionTarget(part.getType(), EnumArmorFacing.fromOutwardNormal(mirror(surface.outwardNormal())), worldSurface, worldPoint.distanceTo(worldSurface), samples));
        }
        targets.sort(Comparator.comparingDouble(VehicleExplosionTarget::distanceMeters).thenComparing(target -> target.part().ordinal()));
        return targets;
    }

    /** Remembers the part a shaped charge has just gone through, so that its blast leaves that part alone. */
    public void markShapedCharge(EnumDriveablePart part, Vec3 point, long gameTime)
    {
        shapedChargeImpact = new ShapedChargeImpact(part, point, gameTime);
    }

    /** @return the part a shaped charge went through at this detonation, reported once */
    @Nullable
    public EnumDriveablePart consumeShapedCharge(Vec3 detonation, long gameTime)
    {
        ShapedChargeImpact impact = shapedChargeImpact;
        shapedChargeImpact = null;
        if (impact == null || impact.gameTime() != gameTime || impact.point().distanceToSqr(detonation) > SHAPED_CHARGE_MATCH_DISTANCE_SQR)
            return null;
        return impact.part();
    }

    /** The boxes as they stand, for drawing: what is drawn is what rounds hit. */
    public List<Outline> outlines()
    {
        List<Outline> outlines = new ArrayList<>(parts.size());
        for (DriveablePart part : parts.values())
        {
            if (!enabled(part))
                continue;
            AABB box = partBoxModelLocal(part);
            Vec3[] corners = new Vec3[8];
            for (int index = 0; index < corners.length; index++)
            {
                Vec3 hull = DriveableProjectileCollision.partPointToHullLocal(corner(box, index), part.getType(), pose.turret().yaw(), pose.turret().pitch(), turretPivot, turretOffset);
                corners[index] = directionToWorld(hull).scale(pose.scale());
            }
            outlines.add(new Outline(corners, hittable(part)));
        }
        return outlines;
    }
}
