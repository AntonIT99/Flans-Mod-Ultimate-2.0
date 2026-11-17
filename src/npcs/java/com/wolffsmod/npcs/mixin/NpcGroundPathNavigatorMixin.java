package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.*;
import noppes.npcs.ai.NpcGroundPathNavigator;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Custom NPCs' ground navigator, made affordable for long-range NPCs and usable for submarines.
 *
 * <ul>
 * <li>A path search reaches as far as its goal needs plus a detour margin, instead of the NPC's whole aggro range: Custom NPCs
 * stores that range where vanilla keeps the search range, so a 512-block sniper otherwise searches a square kilometre.</li>
 * <li>A pursuit keeps its path for a few ticks, where the combat goals ask for a new one every tick while their target moves.
 * The first search of each NPC is offset, so a crowd does not search in the same tick.</li>
 * <li>Following a path, Custom NPCs looks every tick for a straight walk to each node ahead, tracing the blocks on the way.
 * The look is not repeated while the NPC stands in the same block with the same node ahead, for a few ticks: it found
 * what there was to find the first time.</li>
 * <li>A submarine follows its path, which is laid along the surface, by passing below its nodes.</li>
 * </ul>
 */
@Mixin(value = NpcGroundPathNavigator.class, remap = false)
@SuppressWarnings({"AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
public abstract class NpcGroundPathNavigatorMixin extends GroundPathNavigation
{
    /** Ticks a look for shortcuts stands for the block it was made from, after which changed blocks are noticed. */
    @Unique
    private static final int WOLFFSMODNPCS_SHORTCUT_LIFETIME = 10;

    @Unique
    private final NpcPursuit wolffsmodnpcsPursuit = new NpcPursuit();
    /** Whether the next look for a shortcut is the first of its tick, which decides for the whole tick. */
    @Unique
    private boolean wolffsmodnpcsShortcutFirst;
    @Unique
    private boolean wolffsmodnpcsShortcutSkipped;
    /** Where the last look for shortcuts was made from: the path, its node ahead, the NPC's block, and when. */
    @Unique
    @Nullable
    private Path wolffsmodnpcsShortcutPath;
    @Unique
    private int wolffsmodnpcsShortcutNode;
    @Unique
    private long wolffsmodnpcsShortcutBlock;
    @Unique
    private long wolffsmodnpcsShortcutAt;

    protected NpcGroundPathNavigatorMixin(Mob mob, Level level)
    {
        super(mob, level);
    }

    @Shadow
    protected abstract boolean isDirectPathBetweenPoints(Vec3 from, Vec3 to, int sizeX, int sizeY, int sizeZ);

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

    // Shortcuts along the path

    @Inject(method = {"followThePath", "m_7636_"}, at = @At("HEAD"))
    private void wolffsmodnpcsStartShortcutLook(CallbackInfo callback)
    {
        wolffsmodnpcsShortcutFirst = true;
    }

    @Redirect(method = {"followThePath", "m_7636_"},
        at = @At(value = "INVOKE", target = "Lnoppes/npcs/ai/NpcGroundPathNavigator;isDirectPathBetweenPoints(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;III)Z"))
    private boolean wolffsmodnpcsLookForShortcutOnce(NpcGroundPathNavigator navigator, Vec3 from, Vec3 to, int sizeX, int sizeY, int sizeZ)
    {
        if (wolffsmodnpcsShortcutFirst)
        {
            wolffsmodnpcsShortcutFirst = false;
            // A shortcut that was taken moved the node ahead, so the look after it is never skipped. One that only confirmed
            // the node ahead, or none at all, left the path as it was, which skipping the look does as well.
            int node = path == null ? -1 : path.getNextNodeIndex();
            long block = mob.blockPosition().asLong();
            long now = level.getGameTime();
            wolffsmodnpcsShortcutSkipped = path != null && path == wolffsmodnpcsShortcutPath && node == wolffsmodnpcsShortcutNode && block == wolffsmodnpcsShortcutBlock
                && now >= wolffsmodnpcsShortcutAt && now - wolffsmodnpcsShortcutAt < WOLFFSMODNPCS_SHORTCUT_LIFETIME;
            if (!wolffsmodnpcsShortcutSkipped)
            {
                wolffsmodnpcsShortcutPath = path;
                wolffsmodnpcsShortcutNode = node;
                wolffsmodnpcsShortcutBlock = block;
                wolffsmodnpcsShortcutAt = now;
            }
        }
        return !wolffsmodnpcsShortcutSkipped && isDirectPathBetweenPoints(from, to, sizeX, sizeY, sizeZ);
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
