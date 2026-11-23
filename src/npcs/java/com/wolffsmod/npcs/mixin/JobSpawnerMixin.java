package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.NpcSpawnerRange;
import com.wolffsmod.npcs.sim.SpawnerRanges;
import noppes.npcs.roles.JobSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Hooks into Custom NPCs' spawner job:
 * <ul>
 * <li>Range: replaces the fixed 60 blocks a spawned creature may move from the spawner before the spawner removes it with
 * the range for its kind, the spawner's own or else the server's, so aircraft and vehicles are not removed while they
 * manoeuvre round their target. The spawner's own ranges are saved with the job, which its job page sends whole.</li>
 * </ul>
 */
@Mixin(value = JobSpawner.class, remap = false)
public abstract class JobSpawnerMixin implements SpawnerRanges
{
    @Unique
    private final int[] wolffsmodnpcsRanges = new int[NpcSpawnerRange.Kind.values().length];

    // Range

    @Override
    public int wolffsmodnpcsSpawnerRange(NpcSpawnerRange.Kind kind)
    {
        return wolffsmodnpcsRanges[kind.ordinal()];
    }

    @Override
    public void wolffsmodnpcsSetSpawnerRange(NpcSpawnerRange.Kind kind, int blocks)
    {
        wolffsmodnpcsRanges[kind.ordinal()] = NpcSpawnerRange.clamp(blocks);
    }

    @ModifyArg(method = "shouldDelete", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/EntityNPCInterface;isInRange(Lnet/minecraft/world/entity/Entity;D)Z"), index = 1)
    @SuppressWarnings({"unused", "java:S1172"}) // The handler receives the replaced stock range along with the spawned creature.
    private double wolffsmodnpcsSpawnedRange(Entity spawned, double range)
    {
        return NpcSpawnerRange.of(this, spawned);
    }

    @Inject(method = "save", at = @At("TAIL"))
    private void wolffsmodnpcsSaveRanges(CompoundTag compound, CallbackInfoReturnable<CompoundTag> callback)
    {
        NpcSpawnerRange.save(this, compound);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void wolffsmodnpcsLoadRanges(CompoundTag compound, CallbackInfo callback)
    {
        NpcSpawnerRange.load(this, compound);
    }
}
