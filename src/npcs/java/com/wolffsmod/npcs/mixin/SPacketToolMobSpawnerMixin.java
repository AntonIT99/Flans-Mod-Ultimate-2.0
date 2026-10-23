package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.NpcCreationFactions;
import noppes.npcs.packets.PacketServerBasic;
import noppes.npcs.packets.server.SPacketToolMobSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Applies a builder's faction override to NPC clones spawned directly by the cloning tool. */
@Mixin(value = SPacketToolMobSpawner.class, remap = false)
public abstract class SPacketToolMobSpawnerMixin extends PacketServerBasic
{
    @Redirect(method = "handle", at = @At(value = "INVOKE", target = "Lnoppes/npcs/packets/server/SPacketToolMobSpawner;spawnClone(Lnet/minecraft/nbt/CompoundTag;DDDLnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"))
    private Entity wolffsmodnpcsAssignCloneFaction(CompoundTag tag, double x, double y, double z, Level level)
    {
        Entity entity = SPacketToolMobSpawner.spawnClone(tag, x, y, z, level);
        NpcCreationFactions.apply(player, entity);
        return entity;
    }
}
