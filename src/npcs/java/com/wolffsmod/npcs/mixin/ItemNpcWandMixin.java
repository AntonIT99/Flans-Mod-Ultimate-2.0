package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.NpcCreationFactions;
import noppes.npcs.items.ItemNpcWand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Allows creation in water (at the creator's depth when submerged, otherwise on the surface) and applies the creating player's faction default to new NPCs. */
@Mixin(value = ItemNpcWand.class, remap = false)
public abstract class ItemNpcWandMixin extends Item
{
    protected ItemNpcWandMixin(Properties properties)
    {
        super(properties);
    }

    @ModifyVariable(method = {"useOn", "m_6225_"}, at = @At("HEAD"), argsOnly = true)
    private UseOnContext wolffsmodnpcsTargetWaterSurface(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player != null)
        {
            BlockHitResult hit = getPlayerPOVHitResult(context.getLevel(), player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK && context.getLevel().getFluidState(hit.getBlockPos()).is(FluidTags.WATER))
            {
                return new UseOnContext(player, context.getHand(), hit);
            }
        }
        return context;
    }

    @Inject(method = {"use", "m_7203_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsCreateOnWater(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback)
    {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER))
        {
            callback.setReturnValue(new InteractionResultHolder<>(useOn(new UseOnContext(player, hand, hit)), player.getItemInHand(hand)));
        }
    }

    @Redirect(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    @SuppressWarnings("DefaultAnnotationParam") // The vanilla invocation must remap despite the containing mixin's remap = false.
    private boolean wolffsmodnpcsAssignCreatedFaction(Level level, Entity entity, UseOnContext context)
    {
        Player player = context.getPlayer();
        NpcCreationFactions.apply(player, entity);
        if (player != null)
        {
            wolffsmodnpcsPlaceInWater(level, player, entity);
        }
        return level.addFreshEntity(entity);
    }

    /** Places an NPC created in water at the creator's depth when they are submerged, otherwise on the water surface. */
    @Unique
    private static void wolffsmodnpcsPlaceInWater(Level level, Player player, Entity entity)
    {
        BlockPos.MutableBlockPos pos = entity.blockPosition().mutable();
        if (!level.getFluidState(pos).is(FluidTags.WATER))
        {
            return;
        }
        if (player.isUnderWater())
        {
            entity.setPos(entity.getX(), player.getY(), entity.getZ());
            return;
        }
        while (level.getFluidState(pos.above()).is(FluidTags.WATER) && pos.getY() < level.getMaxBuildHeight())
        {
            pos.move(Direction.UP);
        }
        entity.setPos(entity.getX(), pos.getY() + level.getFluidState(pos).getHeight(level, pos), entity.getZ());
    }
}
