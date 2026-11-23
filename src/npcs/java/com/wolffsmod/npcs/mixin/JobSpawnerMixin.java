package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.NpcSpawnerRange;
import noppes.npcs.roles.JobSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.entity.Entity;

/**
 * Hooks into Custom NPCs' spawner job:
 * <ul>
 * <li>Range: replaces the fixed 60 blocks a spawned creature may move from the spawner before the spawner removes it with
 * the configured range for its kind, so aircraft and vehicles are not removed while they manoeuvre round their target.</li>
 * </ul>
 */
@Mixin(value = JobSpawner.class, remap = false)
public abstract class JobSpawnerMixin
{
    // Range

    @ModifyArg(method = "shouldDelete", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/EntityNPCInterface;isInRange(Lnet/minecraft/world/entity/Entity;D)Z"), index = 1)
    @SuppressWarnings({"unused", "java:S1172"}) // The handler receives the replaced stock range along with the spawned creature.
    private double wolffsmodnpcsSpawnedRange(Entity spawned, double range)
    {
        return NpcSpawnerRange.of(spawned);
    }
}
