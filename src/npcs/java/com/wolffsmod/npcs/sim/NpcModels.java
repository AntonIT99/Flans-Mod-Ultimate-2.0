package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.wolffsmod.npcs.combat.NpcMechaTools;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.FlanModelKind;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Where the content-pack model of an NPC stands in the world. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcModels
{
    /** Blocks under which a hitbox is one Custom NPCs switched off. */
    private static final double UNHIT_WIDTH = 0.01D;

    /** @return the content-pack model the NPC is drawn as, or null for any other NPC */
    @Nullable
    public static FlanModelEntity flan(Entity entity)
    {
        return entity instanceof EntityCustomNpc custom && custom.modelData != null && custom.modelData.getEntity(custom) instanceof FlanModelEntity model ? model : null;
    }

    /** @return whether a machine NPC has nothing to strike with; mechas require a sword addon */
    public static boolean meleeless(Entity entity)
    {
        FlanModelEntity model = flan(entity);
        FlanModelKind kind = model == null ? null : FlanModelKind.of(model.getInfoType());
        return kind != null && (kind != FlanModelKind.MECHA || entity instanceof EntityNPCInterface npc && NpcMechaTools.properties(npc).isEmpty());
    }

    /** @return the factor the NPC's size setting scales its model by */
    public static float scale(EntityNPCInterface npc)
    {
        return npc.display.getSize() / 5F;
    }

    /**
     * Custom NPCs makes the hitbox of an NPC that has none, or whose killed body is hidden, a hair wide, and the client
     * culls by that hitbox: a large NPC disappears as soon as the line through its middle leaves the view.
     *
     * @return the hitbox widened to what it would be with a hitbox; empty for an NPC that has its width
     */
    public static Optional<AABB> unhitCullingBox(EntityNPCInterface npc)
    {
        AABB box = npc.getBoundingBox();
        if (box.getXsize() >= UNHIT_WIDTH)
            return Optional.empty();
        Entity model = npc instanceof EntityCustomNpc custom ? custom.modelData.getEntity(custom) : null;
        double half = (model == null ? EntityPlatform.width(npc.baseSize) : model.getBbWidth()) * npc.display.getSize() / 10D;
        return Optional.of(box.inflate(half, 0D, half));
    }

    /** @return how far the NPC's body offset and posture move its model up or down, in blocks */
    public static double bodyOffset(EntityNPCInterface npc)
    {
        double bodyOffset = npc.currentAnimation == 0 ? npc.ais.bodyOffsetY / 10D - 0.5D : 0D;
        if (npc.isPassenger() && npc instanceof EntityCustomNpc custom)
            bodyOffset -= 0.5D - custom.modelData.getLegsY() * 0.8D;
        else if (npc.isCrouching())
            bodyOffset -= 0.125D;
        return bodyOffset * npc.display.getSize() / 5D;
    }

    /**
     * @param offset
     *            a point in the model preview frame: blocks, Y up, forward -Z, relative to the model origin
     * @param hullYaw
     *            the yaw the model is drawn at
     * @return the point relative to the NPC's feet, in world axes, with the pitch and bank of a plane in flight
     */
    public static Vec3 local(EntityNPCInterface npc, FlanModelEntity model, Vec3 offset, float hullYaw)
    {
        FlanModelEntityType type = model.getModelType();
        Vec3 point = offset.add(0D, type == null ? 0F : type.getShape().modelHeight(), 0D);
        if (type != null)
        {
            // Round the middle of the model's box, as it is drawn.
            Vec3 pivot = new Vec3(0D, attitudePivot(type), 0D);
            point = tilt(npc, point.subtract(pivot)).add(pivot);
        }
        return point.scale(scale(npc)).yRot((float) Math.toRadians(180F - hullYaw)).add(0D, bodyOffset(npc), 0D);
    }

    /**
     * @param vector
     *            a direction of the model preview frame
     * @return the direction banked about the model's length, then pitched about its width, as a plane in flight is; still
     *         in the model preview frame
     */
    public static Vec3 tilt(EntityNPCInterface npc, Vec3 vector)
    {
        NpcVehicleState state = NpcVehicleState.of(npc);
        if (state.getAttitudePitch() == 0F && state.getAttitudeBank() == 0F)
            return vector;
        return vector.zRot((float) Math.toRadians(state.getAttitudeBank())).xRot((float) Math.toRadians(state.getAttitudePitch()));
    }

    /** @return blocks above the feet, at size 5, a model pitches and banks about: the middle of its box */
    public static float attitudePivot(FlanModelEntityType type)
    {
        return EntityPlatform.height(type.getShape().dimensions()) / 2F;
    }

    /** @return the world position of a point of the model preview frame */
    public static Vec3 world(EntityNPCInterface npc, FlanModelEntity model, Vec3 offset, float hullYaw)
    {
        return npc.position().add(local(npc, model, offset, hullYaw));
    }

    /**
     * @param aim
     *            where a weapon points, relative to the hull
     * @return the world direction of the aim, pitched and banked with the model as a plane in flight is, as {@link #local}
     *         carries points
     */
    public static Vec3 direction(EntityNPCInterface npc, MountAim aim, float hullYaw)
    {
        // Into the model preview frame, whose forward is -Z, then out of it round the hull's yaw.
        Vec3 forward = Vec3.directionFromRotation(aim.pitch(), aim.yaw()).yRot(Mth.PI);
        return tilt(npc, forward).yRot((float) Math.toRadians(180F - hullYaw));
    }

    /** @return the aim, relative to the hull as it is pitched and banked, that points along a world direction: the inverse of {@link #direction} */
    public static MountAim relative(EntityNPCInterface npc, Vec3 direction, float hullYaw)
    {
        Vec3 model = direction.yRot((float) Math.toRadians(hullYaw - 180F));
        NpcVehicleState state = NpcVehicleState.of(npc);
        if (state.getAttitudePitch() != 0F || state.getAttitudeBank() != 0F)
            model = model.xRot((float) Math.toRadians(-state.getAttitudePitch())).zRot((float) Math.toRadians(-state.getAttitudeBank()));
        return MountMotion.relative(model.yRot(Mth.PI), 0F);
    }
}
