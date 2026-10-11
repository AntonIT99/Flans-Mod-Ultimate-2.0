package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.model.FlanModelEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Where the content-pack model of an NPC stands in the world. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcModels
{
    /** @return the content-pack model the NPC is drawn as, or null for any other NPC */
    @Nullable
    public static FlanModelEntity flan(Entity entity)
    {
        return entity instanceof EntityCustomNpc custom && custom.modelData.getEntity(custom) instanceof FlanModelEntity model ? model : null;
    }

    /** @return the factor the NPC's size setting scales its model by */
    public static float scale(EntityNPCInterface npc)
    {
        return npc instanceof EntityCustomNpc custom && custom.modelData.simpleRender ? 1F : npc.display.getSize() / 5F;
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
