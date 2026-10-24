package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.network.MuzzleFlashPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.sim.NpcModels;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Sends transient flashes only for successful NPC vehicle shots, independently of shooting particles. */
public final class NpcMuzzleFlashes
{
    private NpcMuzzleFlashes()
    {}

    public static void fired(EntityNPCInterface npc, WeaponMuzzle muzzle, Vec3 direction, float hullYaw, boolean secondary)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (!(npc.level() instanceof ServerLevel level) || !level.getServer().isSameThread() || model == null || !(model.getInfoType() instanceof IDriveableType)
            || !FlansProjectiles.hasMuzzleFlash(model.getInfoType(), muzzle, secondary)
            || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(NpcWeaponOptions.Feature.MUZZLE_FLASHES))
            return;
        Vec3 localDirection = direction.normalize().yRot((float) Math.toRadians(hullYaw - 180F));
        NpcNetwork.sendToTracking(npc, new MuzzleFlashPacket(npc.getId(), muzzle.weapon().map(IContentType::getShortName).orElse(""), muzzle.offset(), localDirection));
    }
}
