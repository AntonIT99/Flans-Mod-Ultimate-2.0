package com.flansmodultimate.common.item;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.types.ToolType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ToolItem extends Item implements IFlanItem<ToolType>
{
    public static final String NBT_KEY_STRING = "key";

    @Getter
    protected final ToolType configType;
    protected final String shortname;

    public ToolItem(ToolType configType)
    {
        super(new Properties().stacksTo(1));
        this.configType = configType;
        shortname = configType.getShortName();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_KEY_STRING))
        {
            tooltipComponents.add(Component.literal(tag.getString(NBT_KEY_STRING)).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, @NotNull Level level, @NotNull Player player)
    {
        stack.getOrCreateTag();
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (configType.getFoodness() > 0)
            return super.use(level, player, hand);

        if (isDepleted(stack))
            return InteractionResultHolder.fail(stack);

        // Parachute
        if (configType.isParachute())
        {
            if (!level.isClientSide)
            {
                //TODO: implement
                /*if (ParachuteHooks.canUseParachute(player))
                {
                    ParachuteHooks.spawnAndMountParachute(level, player, configType);
                    consumeUse(stack, player);
                    return InteractionResultHolder.consume(stack);
                }*/
            }
            return InteractionResultHolder.pass(stack);
        }

        // Remote detonator
        if (configType.isRemote())
            return doDetonateRemoteExplosives(level, player, stack);

        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0f);
        double reach = 5.0;
        Vec3 end = start.add(dir.scale(reach));

        // Heal driveables
        if (configType.isHealDriveables())
        {
            if (!level.isClientSide)
            {
                //TODO: implement
                /*boolean healed = DriveableHooks.tryHealDriveableAlongRay(level, start, end, configType.getHealAmount(), () -> isDepleted(stack));
                if (healed) {
                    consumeUse(stack, player);
                    return InteractionResultHolder.consume(stack);
                }*/
            }
            return InteractionResultHolder.pass(stack);
        }

        // Heal players (server)
        if (configType.isHealPlayers())
        {
            if (!level.isClientSide)
            {
                LivingEntity target = pickLivingTarget(level, player, start, end);
                target.heal(configType.getHealAmount());
                //TODO: implement
                //ParticlesHooks.spawnHearts(level, target);
                consumeUse(stack, player);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.pass(stack);
        }

        // Key behavior placeholder
        if (configType.isKey())
        {
            if (!level.isClientSide)
            {
                //TODO: implement
                //KeyHooks.onKeyUsed(level, player, start, end, stack, configType);
                consumeUse(stack, player);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.pass(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    protected InteractionResultHolder<ItemStack> doDetonateRemoteExplosives(Level level, Player player, ItemStack stack)
    {
        if (level.isClientSide)
            return InteractionResultHolder.pass(stack);

        PlayerData data = PlayerData.getInstance(player);
        if (data.getRemoteExplosives().isEmpty())
            return InteractionResultHolder.pass(stack);

        for (Grenade explosive : data.getRemoteExplosives())
        {
            explosive.detonate(level);
            if (explosive.isDetonated())
                data.getRemoteExplosives().remove(explosive);
        }

        if (!player.getAbilities().instabuild && configType.getToolLife() > 0)
            stack.setDamageValue(stack.getDamageValue() + 1);

        if (configType.getToolLife() > 0 && configType.isDestroyOnEmpty() && stack.getDamageValue() == stack.getMaxDamage())
        {
            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.success(stack);
    }

    protected boolean hasFiniteUses()
    {
        return configType.getToolLife() > 0;
    }

    protected boolean isDepleted(ItemStack stack)
    {
        return hasFiniteUses() && stack.getDamageValue() >= configType.getToolLife();
    }

    protected void consumeUse(ItemStack stack, Player player)
    {
        if (!hasFiniteUses())
            return;
        if (player.getAbilities().instabuild)
            return;

        stack.setDamageValue(stack.getDamageValue() + 1);

        if (configType.isDestroyOnEmpty() && stack.getDamageValue() >= configType.getToolLife())
            stack.shrink(1);
    }

    protected LivingEntity pickLivingTarget(Level level, Player user, Vec3 start, Vec3 end)
    {
        LivingEntity chosen = user;
        Vec3 delta = end.subtract(start);

        AABB searchBox = user.getBoundingBox()
            .expandTowards(delta)
            .inflate(1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, user, start, end, searchBox, e -> e instanceof LivingEntity living && living != user);
        if (hit != null && hit.getEntity() instanceof LivingEntity living)
            chosen = living;

        return chosen;
    }
}
