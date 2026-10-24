package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.client.equipment.FlansMuzzleFlashes;
import com.flansmodultimate.api.content.IContentType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.network.MuzzleFlashPacket;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.entity.EntityNPCInterface;
import org.joml.Quaternionf;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Short-lived shot visuals, following the NPC's movement and hull rotation. Never replayed when tracking begins. */
public final class NpcMuzzleFlashRenderer
{
    private static final Map<EntityNPCInterface, List<Flash>> ACTIVE = new WeakHashMap<>();
    private static final int FLASH_TICKS = 2;

    private record Flash(MuzzleFlashPacket shot, IContentType weapon, int started)
    {}

    private NpcMuzzleFlashRenderer()
    {}

    public static void receive(MuzzleFlashPacket packet)
    {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(packet.entityId()) instanceof EntityNPCInterface npc) || packet.direction().lengthSqr() < 1.0E-8D)
            return;
        List<Flash> flashes = ACTIVE.computeIfAbsent(npc, ignored -> new ArrayList<>());
        flashes.removeIf(flash -> flash.started + FLASH_TICKS < npc.tickCount || flash.shot.offset().equals(packet.offset()));
        flashes.add(new Flash(packet, FlansModApi.getType(packet.weapon()).orElse(null), npc.tickCount));
    }

    public static void clear()
    {
        ACTIVE.clear();
    }

    public static void render(PoseStack pose, Camera camera, float partialTick)
    {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ACTIVE.entrySet().removeIf(entry -> entry.getKey().isRemoved() || entry.getKey().level() != minecraft.level);
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (ACTIVE.isEmpty())
            return;
        for (var entry : ACTIVE.entrySet())
        {
            EntityNPCInterface npc = entry.getKey();
            FlanModelEntity model = NpcModels.flan(npc);
            entry.getValue().removeIf(flash -> flash.started + FLASH_TICKS < npc.tickCount);
            if (model == null || npc.isInvisible() || npc.isKilled())
                continue;
            float hullYaw = NpcVehicleState.of(npc).getHullYaw();
            for (Flash flash : entry.getValue())
            {
                Vec3 origin = npc.getPosition(partialTick).add(NpcModels.local(npc, model, flash.shot.offset(), hullYaw)).subtract(camera.getPosition());
                Vec3 direction = flash.shot.direction().yRot((float) Math.toRadians(180F - hullYaw));
                pose.pushPose();
                pose.translate(origin.x, origin.y, origin.z);
                pose.mulPose(new Quaternionf().rotationTo(1F, 0F, 0F, (float) direction.x, (float) direction.y, (float) direction.z));
                FlansMuzzleFlashes.render(flash.weapon, model.getInfoType(), npc.tickCount - flash.started, NpcModels.scale(npc), pose, buffers);
                pose.popPose();
            }
        }
        buffers.endBatch();
    }
}
