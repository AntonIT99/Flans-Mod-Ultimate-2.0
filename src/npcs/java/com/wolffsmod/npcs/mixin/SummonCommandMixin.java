package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.NpcCreationFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/** Gives NPCs summoned by a player, including passengers, that player's faction override before adding them to the world. */
@Mixin(SummonCommand.class)
@SuppressWarnings("java:S1118") // This class contains static mixin hooks, not a standalone utility.
public abstract class SummonCommandMixin
{
    @Redirect(method = "createEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean wolffsmodnpcsAssignSummonedFaction(ServerLevel level, Entity entity, CommandSourceStack source, Holder.Reference<EntityType<?>> type, Vec3 position, CompoundTag tag,
        boolean randomizeProperties)
    {
        NpcCreationFactions.applyCommand(source, entity);
        return level.tryAddFreshEntityWithPassengers(entity);
    }
}
