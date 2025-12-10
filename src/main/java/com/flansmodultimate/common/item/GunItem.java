package com.flansmodultimate.common.item;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelGun;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.ModClient;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.EnumSecondaryFunction;
import com.flansmodultimate.common.guns.FireDecision;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModClientConfigs;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.network.server.PacketReload;
import com.flansmodultimate.util.ModUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.Getter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;

public class GunItem extends Item implements IPaintableItem<GunType>, ICustomRendererItem<GunType>
{
    protected static final String NBT_AMMO = "ammo";
    protected static final String NBT_SECONDARY_AMMO = "secondary_ammo";
    protected static final String NBT_LEGENDARY_CRAFTER = "legendary_crafter";

    protected static boolean rightMouseHeld;
    protected static boolean lastRightMouseHeld;
    protected static boolean leftMouseHeld;
    protected static boolean lastLeftMouseHeld;
    @Getter
    protected static boolean crouching;
    protected static boolean sprinting;
    protected static boolean shooting;

    @Getter
    protected final GunType configType;
    protected final String shortname;
    @Getter
    protected final GunItemBehavior behavior;
    protected String originGunbox;
    protected int soundDelay;
    @Getter
    protected int impactX;
    @Getter
    protected int impactY;
    @Getter
    protected int impactZ;
    //TODO: implement checkForLockOn

    public GunItem(GunType configType)
    {
        super(new Properties().stacksTo(1));
        this.configType = configType;
        shortname = configType.getShortName();
        behavior = new GunItemBehavior(this);
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer)
    {
        ICustomRendererItem.super.initializeClient(consumer);
    }

    @Override
    public boolean useCustomRendererInHand()
    {
        return ModelCache.getOrLoadTypeModel(configType) != null;
    }

    @Override
    public boolean useCustomRendererOnGround()
    {
        return ModelCache.getOrLoadTypeModel(configType) != null;
    }

    @Override
    public boolean useCustomRendererInFrame()
    {
        return ModelCache.getOrLoadTypeModel(configType) != null;
    }

    @Override
    public boolean useCustomRendererInGui()
    {
        return false;
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        IModelBase model = ModelCache.getOrLoadTypeModel(configType);
        if (model instanceof ModelGun modelGun)
        {
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(getPaintjob(stack).getTexture()));
            int color = configType.getColour();
            float red = (color >> 16 & 255) / 255F;
            float green = (color >> 8 & 255) / 255F;
            float blue = (color & 255) / 255F;
            modelGun.renderItem(stack, itemDisplayContext, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, 1F);
        }
    }

    public boolean useAimingAnimation()
    {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);

        // Legendary crafter tag
        if (stack.hasTag() && stack.getTag() != null && stack.getTag().contains(NBT_LEGENDARY_CRAFTER, Tag.TAG_STRING))
        {
            String crafter = stack.getTag().getString(NBT_LEGENDARY_CRAFTER);
            tooltipComponents.add(Component.literal("Legendary Skin Crafted by " + crafter).withStyle(ChatFormatting.GOLD));
        }

        if (!Screen.hasShiftDown())
        {
            // Attachments
            if (configType.isShowAttachments())
            {
                List<AttachmentType> attachments = configType.getCurrentAttachments(stack);

                if (!attachments.isEmpty())
                    tooltipComponents.add(Component.literal("Attachments").withStyle(ChatFormatting.YELLOW));

                for (AttachmentType attachment : attachments)
                    tooltipComponents.add(Component.literal(attachment.getName()).withStyle(ChatFormatting.AQUA));
            }


            // Ammo info
            for (int i = 0; i < configType.getNumAmmoItemsInGun(stack); i++)
            {
                ItemStack bulletStack = getBulletItemStack(stack, i);
                if (bulletStack != null && !bulletStack.isEmpty() && bulletStack.getItem() instanceof BulletItem bulletItem)
                {
                    BulletType bulletType = bulletItem.getConfigType();
                    int max = bulletStack.getMaxDamage();
                    int remaining = max - bulletStack.getDamageValue();
                    String line = bulletType.getName() + " " + remaining + "/" + max;
                    tooltipComponents.add(Component.literal(line).withStyle(ChatFormatting.DARK_BLUE));
                }
            }

            tooltipComponents.add(Component.empty());

            KeyMapping shiftKey = Minecraft.getInstance().options.keyShift;
            Component keyName = shiftKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            AttachmentType barrel = configType.getBarrel(stack);
            if (barrel != null && barrel.isSilencer())
                tooltipComponents.add(Component.literal("[Suppressed]").withStyle(ChatFormatting.YELLOW));

            if (configType.getSecondaryFire(stack))
                tooltipComponents.add(Component.literal("[Underbarrel]").withStyle(ChatFormatting.YELLOW));

            if (StringUtils.isNotBlank(originGunbox))
                tooltipComponents.add(IFlanItem.statLine("Box", originGunbox));

            // Stats
            if (configType.isShowDamage())
            {
                //TODO: for old damage system: damage vs Entity colorized?
                tooltipComponents.add(IFlanItem.statLine("Damage", StringUtils.EMPTY));
                for (ShootableType shootableType : configType.getAmmoTypes())
                    tooltipComponents.add(IFlanItem.indentedStatLine(ModUtils.getItemLocalizedName(shootableType.getShortName()), IFlanItem.formatFloat(shootableType.getDamageForDisplay(configType, stack))));
            }

            if (configType.isShowRecoil())
            {
                tooltipComponents.add(IFlanItem.statLine("Vertical Recoil", IFlanItem.formatFloat(configType.getDisplayVerticalRecoil(stack))));
                tooltipComponents.add(IFlanItem.statLine("Horizontal Recoil", IFlanItem.formatFloat(configType.getDisplayHorizontalRecoil(stack))));

                String sprintingControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, true, false));
                String sneakingControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, false, true));
                String normalControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, false, false));

                Component recoilControl = Component.literal("Recoil Control: ").withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(" " + sprintingControl).withStyle(ChatFormatting.RED))
                        .append(Component.literal(" " + normalControl).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" " + sneakingControl).withStyle(ChatFormatting.GREEN));
                tooltipComponents.add(recoilControl);
            }

            if (configType.isShowSpread())
            {
                float spread = configType.getSpread(stack, false, false);
                tooltipComponents.add(IFlanItem.statLine("Accuracy", IFlanItem.formatFloat(spread)));
                tooltipComponents.add(IFlanItem.statLine("Dispersion", IFlanItem.formatFloat(Mth.RAD_TO_DEG * ShootingHelper.ANGULAR_SPREAD_FACTOR * spread) + "°"));
            }

            if (configType.getSwitchDelay() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Switch Delay", IFlanItem.formatFloat(configType.getSwitchDelay())));

            if (configType.isShowReloadTime())
                tooltipComponents.add(IFlanItem.statLine("Reload Time", IFlanItem.formatFloat(configType.getReloadTime(stack) / 20F) + "s"));

            float bulletSpeed = configType.getBulletSpeed(stack);
            tooltipComponents.add(IFlanItem.statLine("Muzzle Velocity", (bulletSpeed != 0F) ? (IFlanItem.formatFloat(bulletSpeed * 20F) + "m/s") : "∞"));

            tooltipComponents.add(IFlanItem.statLine("FireRate", (1200 / configType.getShootDelay(stack)) + "rpm"));

            tooltipComponents.add(IFlanItem.statLine("Mode", configType.getFireMode(stack).name().toLowerCase()));
        }
    }

    @Override
    public PaintableType getPaintableType()
    {
        return configType;
    }

    /**
     * Called when the player left-clicks an entity with this item.
     * Return true to cancel the hit (no damage), false to allow normal attack.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target)
    {
        // Stop damage when scoping etc.
        return configType.getSecondaryFunction() != EnumSecondaryFunction.MELEE;
    }

    //TODO: implement custom entity
    /**
     * Allows replacing the default ItemEntity with a custom entity when this stack
     * exists in the world (dropped on ground).
     * Return true to call {@link #createEntity} for the replacement.
     *//*
    @Override
    public boolean hasCustomEntity(ItemStack stack)
    {
        return true;
    }

    *//**
     * Creates a custom entity to replace the original dropped ItemEntity.
     * The 'location' is the original entity about to be spawned/replaced (usually an ItemEntity).
     *//*
    @Override
    @Nullable
    public Entity createEntity(Level level, Entity location, ItemStack stack)
    {
        // Example: spawn at original entity's position; adjust to your custom entity’s ctor.
        // If your class extends ItemEntity, you can copy motion/owner as needed.
        return new EntityItemCustomRender(level, location.getX(), location.getY(), location.getZ(), stack);
    }*/

    /**
     * Called when this item is swung. Return true to cancel further swing logic/animation.
     * Use client check for client-only animation/sounds.
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity)
    {
        Level level = entity.level();

        if (configType.getMeleeSound() != null)
        {
            // Your packet/util for playing a sound (server -> clients near position)
            PacketPlaySound.sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), FlansMod.SOUND_RANGE, level.dimension(), configType.getMeleeSound(), true);
        }

        if (configType.getSecondaryFunction() == EnumSecondaryFunction.CUSTOM_MELEE)
        {
            // Client-side animation
            if (level.isClientSide)
            {
                GunAnimations anim = ModClient.getGunAnimations(entity, InteractionHand.MAIN_HAND);
                anim.doMelee(configType.getMeleeTime());
            }
            // Server-side custom melee hit detection/logic
            if (entity instanceof Player player)
            {
                PlayerData data = PlayerData.getInstance(player);
                data.doMelee(player, configType.getMeleeTime(), configType);
            }
        }
        // Keep vanilla hit if and only if we’re in MELEE mode; otherwise cancel to prevent damage
        return configType.getSecondaryFunction() != EnumSecondaryFunction.MELEE;
    }

    /**
     * Called when the player starts breaking a block with this item.
     * Return true to cancel further processing (and client-side breaking animation).
     * We bounce a block update on the server to ensure proper visuals in creative.
     */
    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player)
    {
        Level level = player.level();
        if (!level.isClientSide)
        {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
        return true;
    }

    /**
     * Whether this item is an appropriate tool for drops from the given block.
     * (Replacement for 1.12's canHarvestBlock.)
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state)
    {
        return false;
    }

    /**
     * Controls whether equipping a new stack in the same slot should play the re-equip animation.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged;
    }

    /**
     * Forbid attacking blocks with this item.
     */
    @Override
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player)
    {
        // Return false to prevent left-click block breaking with this item (even in survival).
        return false;
    }

    /**
     * Deployable guns only
     */
    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand)
    {
        //TODO: implement deployables
        ItemStack stack = player.getItemInHand(hand);
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Generic update method. If we have an offhand weapon, it will also make calls for that.
     */
    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected)
    {
        if (!(entity instanceof Player player))
            return;

        // Figure out which hand holds this stack.
        // Not held -> ignore
        InteractionHand hand;
        if (player.getMainHandItem() == stack)
            hand = InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem() == stack)
            hand = InteractionHand.OFF_HAND;
        else
            return;

        pollMouseButtonStateWhenNoScreen(level);

        boolean hasOffHand = !player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty();

        if (level.isClientSide)
            onUpdateClient(stack, slotId, level, player, hand, hasOffHand);
        else
            onUpdateServer(stack, slotId, level, player, hand, hasOffHand);

        //TODO from FMUltimate
        //checkForLockOn()
        //checkForMelee()
    }

    @OnlyIn(Dist.CLIENT)
    protected void onUpdateClient(ItemStack gunStack, int gunSlot, Level level, @NotNull Player player, InteractionHand hand, boolean hasOffHand)
    {
        //TODO: implement FMU stuff

        // Not for deployables
        if (configType.isDeployable())
            return;

        // Scope handling
        behavior.handleScopeToggleIfNeeded(gunStack, hand, hasOffHand);

        // Grab per-player data and input edge
        PlayerData data = PlayerData.getInstance(player);
        data.setMinigunSpeed(data.getMinigunSpeed() * 0.9F); // slow down minigun each tick

        boolean hold = getMouseHeld(hand);
        boolean held = getLastMouseHeld(hand);

        // Don’t shoot certain entities under crosshair
        if (behavior.shouldBlockFireAtCrosshair())
            hold = false;

        // Idle sound (TODO: ideally server-side)
        behavior.playIdleSoundIfAny(level, player);

        if (!behavior.gunCanBeHandled(player))
            return;
        if (!configType.isUsableByPlayers())
            return;

        // Fire-mode decision
        GunAnimations anim = ModClient.getGunAnimations(player, hand);
        FireDecision decision = FireDecision.computeFireDecision(this, gunStack, hand, data, hold, held, anim);

        if (decision.needsReload())
            PacketHandler.sendToServer(new PacketReload(hand, false));
        else if (decision.shouldShoot())
            behavior.shoot(hand, player, gunStack, data, level, anim);
    }

    protected void onUpdateServer(ItemStack gunStack, int gunSlot, Level level, @NotNull Player player, InteractionHand hand, boolean hasOffHand)
    {
        //TODO: implement FMU stuff
        PlayerData data = PlayerData.getInstance(player);

        //If the player is no longer holding a gun, emulate a release of the shoot button
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof GunItem))
            data.setShootingRight(false);
        if (offHand.isEmpty() || !(offHand.getItem() instanceof GunItem))
            data.setShootingLeft(false);

        // And finally do sounds
        if(soundDelay > 0)
            soundDelay--;
    }

    protected static boolean getMouseHeld(InteractionHand hand)
    {
        if (BooleanUtils.isTrue(ModClientConfigs.shootOnRightClick.get()))
            return hand == InteractionHand.MAIN_HAND ? rightMouseHeld : leftMouseHeld;
        else
            return hand == InteractionHand.MAIN_HAND ? leftMouseHeld : rightMouseHeld;
    }

    protected static boolean getLastMouseHeld(InteractionHand hand)
    {
        if (BooleanUtils.isTrue(ModClientConfigs.shootOnRightClick.get()))
            return hand == InteractionHand.MAIN_HAND ? lastRightMouseHeld : lastLeftMouseHeld;
        else
            return hand == InteractionHand.MAIN_HAND ? lastLeftMouseHeld : lastRightMouseHeld;
    }

    protected static void pollMouseButtonStateWhenNoScreen(Level level)
    {
        // Client-side input read (only when no GUI is open)
        if (level.isClientSide && Minecraft.getInstance().screen == null)
        {
            Minecraft mc = Minecraft.getInstance();
            lastRightMouseHeld = rightMouseHeld;
            lastLeftMouseHeld  = leftMouseHeld;
            rightMouseHeld = mc.options.keyUse.isDown();
            leftMouseHeld  = mc.options.keyAttack.isDown();
        }
    }

    /**
     * Get the bullet item stack stored in the gun's NBT data (the loaded magazine / bullets).
     * @param id: some guns use multiple bullet items instead of one magazine, id is here the index to identify which one.
     */
    public ItemStack getBulletItemStack(ItemStack gun, int id) {
        if (gun.isEmpty())
            return ItemStack.EMPTY;

        CompoundTag tag = gun.getTag();
        if (tag == null)
        {
            gun.setTag(new CompoundTag());
            return ItemStack.EMPTY;
        }

        String nbt = configType.getSecondaryFire(gun) ? NBT_SECONDARY_AMMO : NBT_AMMO;

        if (!tag.contains(nbt, Tag.TAG_LIST))
        {
            ListTag list = new ListTag();
            for (int i = 0; i < configType.getNumAmmoItemsInGun(gun); i++)
                list.add(new CompoundTag());
            tag.put(nbt, list);
            return ItemStack.EMPTY;
        }

        ListTag list = tag.getList(nbt, Tag.TAG_COMPOUND);
        if (id < 0 || id >= list.size())
            return ItemStack.EMPTY;

        CompoundTag slotTag = list.getCompound(id);
        return ItemStack.of(slotTag);
    }

    /**
     * Set the bullet item stack stored in the gun's NBT data (the loaded magazine / bullets).
     * @param id: some guns use multiple bullet items instead of one magazine, id is here the index to identify which one.
     */
    public void setBulletItemStack(ItemStack gun, ItemStack bullet, int id) {
        if (gun.isEmpty() || id < 0)
            return;

        ListTag list;
        CompoundTag tag = gun.getOrCreateTag();
        String nbt = configType.getSecondaryFire(gun) ? NBT_SECONDARY_AMMO : NBT_AMMO;

        if (tag.contains(nbt, Tag.TAG_LIST))
        {
            list = tag.getList(nbt, Tag.TAG_COMPOUND);
        }
        else
        {
            list = new ListTag();
            for (int i = 0; i < configType.getNumAmmoItemsInGun(gun); i++)
                list.add(new CompoundTag());
            tag.put(nbt, list);
        }

        while (id >= list.size())
            list.add(new CompoundTag());

        CompoundTag slotTag = (bullet == null || bullet.isEmpty()) ? new CompoundTag() : bullet.save(new CompoundTag());

        list.set(id, slotTag);
        tag.put(nbt, list);
    }
}
