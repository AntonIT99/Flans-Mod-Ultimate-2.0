package com.wolffsmod.npcs.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.wolffsmod.npcs.teams.NpcCreationFactions;
import noppes.npcs.command.CmdClone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Gives NPCs created by /noppes clone spawn and grid the executing player's faction override. */
@Mixin(value = CmdClone.class, remap = false)
@SuppressWarnings({"java:S1118", "DefaultAnnotationParam"}) // Static mixin hooks are not utilities; vanilla calls must remap inside this non-remapped mixin.
public abstract class CmdCloneMixin
{
    @Redirect(method = "spawnClone", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private static boolean wolffsmodnpcsAssignCommandFaction(Level level, Entity entity, CommandContext<CommandSourceStack> context, BlockPos position, String name)
    {
        NpcCreationFactions.applyCommand(context.getSource(), entity);
        return level.addFreshEntity(entity);
    }
}
