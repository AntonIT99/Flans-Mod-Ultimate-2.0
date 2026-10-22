package com.wolffsmod.npcs.sim;

import com.flansmodultimate.platform.entity.EntityPlatform;
import com.wolffsmod.npcs.model.FlanModelEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

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
        return entity instanceof EntityCustomNpc custom && custom.modelData.getEntity(custom) instanceof FlanModelEntity model ? model : null;
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
     * @return the point relative to the NPC's feet, in world axes
     */
    public static Vec3 local(EntityNPCInterface npc, FlanModelEntity model, Vec3 offset, float hullYaw)
    {
        float height = model.getModelType() == null ? 0F : model.getModelType().getShape().modelHeight();
        return offset.add(0D, height, 0D).scale(scale(npc)).yRot((float) Math.toRadians(180F - hullYaw)).add(0D, bodyOffset(npc), 0D);
    }

    /** @return the world position of a point of the model preview frame */
    public static Vec3 world(EntityNPCInterface npc, FlanModelEntity model, Vec3 offset, float hullYaw)
    {
        return npc.position().add(local(npc, model, offset, hullYaw));
    }
}
