package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.*;
import noppes.npcs.ai.NpcGroundPathNavigator;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Set;

/**
 * Custom NPCs' ground navigator, made affordable for long-range NPCs and usable for submarines.
 *
 * <ul>
 * <li>A path search reaches as far as its goal needs plus a detour margin, instead of the NPC's whole aggro range: Custom NPCs
 * stores that range where vanilla keeps the search range, so a 512-block sniper otherwise searches a square kilometre.</li>
 * <li>A pursuit keeps its path for a few ticks, where the combat goals ask for a new one every tick while their target moves.
 * The first search of each NPC is offset, so a crowd does not search in the same tick.</li>
 * <li>A submarine follows its path, which is laid along the surface, by passing below its nodes.</li>
 * </ul>
 */
@Mixin(value = NpcGroundPathNavigator.class, remap = false)
@SuppressWarnings({"AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
public abstract class NpcGroundPathNavigatorMixin extends GroundPathNavigation
{
    @Unique
    private final NpcPursuit wolffsmodnpcsPursuit = new NpcPursuit();

    protected NpcGroundPathNavigatorMixin(Mob mob, Level level)
    {
        super(mob, level);
    }

    // Path searches

    @Override
    public boolean moveTo(@NotNull Entity entity, double speed)
    {
        if (wolffsmodnpcsPursuit.keeps(mob, entity, level.getGameTime(), path != null && !path.isDone()))
        {
            speedModifier = speed;
            return true;
        }
        return super.moveTo(entity, speed);
    }

    @Nullable
    @Override
    protected Path createPath(@NotNull Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange)
    {
        return super.createPath(targets, regionOffset, offsetUpward, accuracy, NpcMobility.searchRange(mob, targets, followRange));
    }

    // Submarines

    @Inject(method = {"followThePath", "m_7636_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsPassBelowNodes(CallbackInfo callback)
    {
        if (path == null || path.isDone() || !(mob instanceof EntityNPCInterface npc) || !NpcVehicleState.of(npc).getSubmarine().isSubmerged())
            return;
        Vec3i node = path.getNextNodePos();
        if (!NpcSubmarine.passesBelow(mob.getX() - (node.getX() + 0.5D), mob.getY() - node.getY(), mob.getZ() - (node.getZ() + 0.5D), mob.getBbWidth()))
            return;
        path.advance();
        // Custom NPCs' own path following takes for granted that there is a next node.
        if (path.isDone())
            callback.cancel();
    }
}
