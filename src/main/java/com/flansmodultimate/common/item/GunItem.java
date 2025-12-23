package com.flansmodultimate.common.item;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelGun;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.input.GunInputState;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.guns.EnumFireDecision;
import com.flansmodultimate.common.guns.EnumSecondaryFunction;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunShootClient;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.network.server.PacketShootInput;
import com.flansmodultimate.util.ModUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.stream.IntStream;

public class GunItem extends Item implements IPaintableItem<GunType>, ICustomRendererItem<GunType>
{
    public static final int LOCK_ON_SOUND_RANGE = 10;

    protected static final String NBT_AMMO = "ammo";
    protected static final String NBT_SECONDARY_AMMO = "secondary_ammo";
    protected static final String NBT_PREFERRED_AMMO = "preferred_ammo";
    protected static final String NBT_PAINT = "paint";
    protected static final String NBT_LEGENDARY_CRAFTER = "legendary_crafter";
    protected static final String NBT_ENTITY_LOCK_ON = "lock_on";

    @Getter
    protected final GunType configType;
    protected final String shortname;
    @Getter
    protected final GunItemHandler gunItemHandler;
    protected String originGunbox;
    @Setter
    protected boolean isScoped;
    protected int soundDelay;
    @Getter
    protected int lockOnSoundDelay;
    @Getter
    protected int impactX;
    @Getter
    protected int impactY;
    @Getter
    protected int impactZ;

    public GunItem(GunType configType)
    {
        super(new Properties().stacksTo(1));
        this.configType = configType;
        shortname = configType.getShortName();
        gunItemHandler = new GunItemHandler(this);
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
            for (ItemStack bulletStack : getBulletItemStackList(stack))
            {
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
            tooltipComponents.add(Component.empty());

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
                tooltipComponents.add(IFlanItem.statLine("Damage", StringUtils.EMPTY));
                for (ShootableType shootableType : configType.getAmmoTypes())
                {
                    if (shootableType.useKineticDamageSystem())
                    {
                        tooltipComponents.add(IFlanItem.indentedStatLine(ModUtils.getItemLocalizedName(shootableType.getShortName()), IFlanItem.formatFloat(shootableType.getDamageForDisplay(configType, stack, null))));
                    }
                    else
                    {
                        float damage = shootableType.getDamageForDisplay(configType, stack, null);
                        MutableComponent damageComponent = IFlanItem.indentedStatLine(ModUtils.getItemLocalizedName(shootableType.getShortName()), IFlanItem.formatFloat(damage));

                        final float EPS = 0.0001F;

                        // vs Living: only show if explicitly configured AND different from base
                        if (shootableType.getDamage().isReadDamageVsLiving() && Math.abs(damage - shootableType.getDamageForDisplay(configType, stack, LivingEntity.class)) > EPS)
                            damageComponent.append(Component.literal(" " + IFlanItem.formatFloat(shootableType.getDamageForDisplay(configType, stack, LivingEntity.class))).withStyle(ChatFormatting.GREEN));

                        // vs Player: inherits from vsLiving
                        if (shootableType.getDamage().isReadDamageVsPlayer() && Math.abs(shootableType.getDamageForDisplay(configType, stack, Player.class) - shootableType.getDamageForDisplay(configType, stack, LivingEntity.class)) > EPS)
                            damageComponent.append(Component.literal(" " + IFlanItem.formatFloat(shootableType.getDamageForDisplay(configType, stack, Player.class))).withStyle(ChatFormatting.RED));

                        // vs Vehicle: inherits from base
                        if (shootableType.getDamage().isReadDamageVsVehicles() && Math.abs(shootableType.getDamageForDisplay(configType, stack, Vehicle.class) - damage) > EPS)
                            damageComponent.append(Component.literal(" " + IFlanItem.formatFloat(shootableType.getDamageForDisplay(configType, stack, Vehicle.class))).withStyle(ChatFormatting.AQUA));

                        // vs Plane: inherits from vsVehicle
                        if (shootableType.getDamage().isReadDamageVsPlanes() && Math.abs(shootableType.getDamageForDisplay(configType, stack, Plane.class) - shootableType.getDamageForDisplay(configType, stack, Vehicle.class)) > EPS)
                            damageComponent.append(Component.literal(" " + IFlanItem.formatFloat(shootableType.getDamageForDisplay(configType, stack, Plane.class))).withStyle(ChatFormatting.LIGHT_PURPLE));

                        tooltipComponents.add(damageComponent);
                    }
                }
            }

            if (configType.isShowRecoil())
            {
                tooltipComponents.add(IFlanItem.statLine("Vertical Recoil", IFlanItem.formatFloat(configType.getDisplayVerticalRecoil(stack))));
                tooltipComponents.add(IFlanItem.statLine("Horizontal Recoil", IFlanItem.formatFloat(configType.getDisplayHorizontalRecoil(stack))));

                String sprintingControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, true, false));
                String sneakingControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, false, true));
                String normalControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, false, false));

                Component recoilControl = Component.literal("Recoil Control: ").withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(sprintingControl).withStyle(ChatFormatting.RED))
                        .append(Component.literal(" " + normalControl).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" " + sneakingControl).withStyle(ChatFormatting.GREEN));
                tooltipComponents.add(recoilControl);
            }

            if (configType.isShowSpread())
                tooltipComponents.add(IFlanItem.statLine("Dispersion", IFlanItem.formatFloat(Mth.RAD_TO_DEG * ShootingHelper.ANGULAR_SPREAD_FACTOR * configType.getSpread(stack, false, false)) + "°"));

            if (configType.getSwitchDelay() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Switch Delay", IFlanItem.formatFloat(configType.getSwitchDelay())));

            if (configType.isShowReloadTime())
                tooltipComponents.add(IFlanItem.statLine("Reload Time", IFlanItem.formatFloat(configType.getReloadTime(stack) / 20F) + "s"));

            float bulletSpeed = configType.getBulletSpeed(stack);
            tooltipComponents.add(IFlanItem.statLine("Muzzle Velocity", (bulletSpeed != 0F) ? (IFlanItem.formatFloat(bulletSpeed * 20F) + "m/s") : "∞"));

            tooltipComponents.add(IFlanItem.statLine("Fire Rate", IFlanItem.formatFloat(1200F / configType.getShootDelay(stack)) + "rpm"));

            tooltipComponents.add(IFlanItem.statLine("Mode", configType.getFireMode(stack).name().toLowerCase()));

            if (configType.getKnockback() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Shooter Knockback", IFlanItem.formatFloat(configType.getKnockback())));
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

        if (!level.isClientSide && StringUtils.isNotBlank(configType.getMeleeSound()))
            PacketPlaySound.sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), configType.getMeleeSoundRange(), level.dimension(), configType.getMeleeSound(), true);

        if (configType.getSecondaryFunction() == EnumSecondaryFunction.CUSTOM_MELEE)
        {
            if (level.isClientSide)
            {
                GunAnimations anim = ModClient.getGunAnimations(entity, InteractionHand.MAIN_HAND);
                anim.doMelee(configType.getMeleeTime());
            }
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

        // Only tick item if in hand
        InteractionHand hand;
        if (player.getMainHandItem() == stack)
            hand = InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem() == stack)
            hand = InteractionHand.OFF_HAND;
        else
            return;

        boolean dualWield = !player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty();
        PlayerData data = PlayerData.getInstance(player);

        if (level.isClientSide)
            onUpdateClient(level, player, data, stack, hand, dualWield);
        else
            onUpdateServer(level, (ServerPlayer) player, data, stack, hand, dualWield);

        gunItemHandler.handleMinigunEffects(level, player, data, configType.getFireMode(stack), hand);
        gunItemHandler.checkForLockOn(level, player, data, hand);
        gunItemHandler.checkForMelee(level, player, data, stack);
    }

    @OnlyIn(Dist.CLIENT)
    protected void onUpdateClient(Level level, @NotNull Player player, @NotNull PlayerData data, ItemStack gunStack, InteractionHand hand, boolean dualWield)
    {
        if (configType.isDeployable() || !configType.isUsableByPlayers() || !gunItemHandler.gunCanBeHandled(player))
            return;

        // Scope handling
        gunItemHandler.handleScope(gunStack, hand, dualWield);

        GunAnimations animations = ModClient.getGunAnimations(player, hand);
        
        // Switch Delay
        gunItemHandler.handleGunSwitchDelay(data, animations, hand);

        // Client Shooting
        if (data.isShooting(hand))
            gunItemHandler.doPlayerShootClient(level, player, data, animations, gunStack, hand);

        boolean shootKeyPressed = GunInputState.isShootPressed(hand);
        boolean prevShootKeyPressed = GunInputState.isPrevShootPressed(hand);

        // Shooting input handling
        if (Minecraft.getInstance().screen != null)
        {
            // Stop shooting when entering a GUI
            if (prevShootKeyPressed)
                PacketHandler.sendToServer(new PacketShootInput(false, true, hand));
        }
        else if (ModClient.getSwitchTime() <= 0)
        {
            // Don’t shoot certain entities under crosshair
            if (gunItemHandler.shouldBlockFireAtCrosshair())
                shootKeyPressed = false;

            data.setShootKeyPressed(hand, shootKeyPressed);
            data.setPrevShootKeyPressed(hand, prevShootKeyPressed);

            if (shootKeyPressed != prevShootKeyPressed)
                PacketHandler.sendToServer(new PacketShootInput(shootKeyPressed, prevShootKeyPressed, hand));
        }
    }

    protected void onUpdateServer(Level level, @NotNull ServerPlayer player, @NotNull PlayerData data, ItemStack gunStack, InteractionHand hand, boolean dualWield)
    {
        ensureGunTags(gunStack);

        EnumFireDecision decision = gunItemHandler.computeFireDecision(data, gunStack, hand);
        if (decision == EnumFireDecision.RELOAD)
            gunItemHandler.doPlayerReload(level, player, data, gunStack, hand, false);
        else if (decision == EnumFireDecision.SHOOT)
            gunItemHandler.doPlayerShoot(level, player, data, gunStack, hand);

        // Stop shooting
        if (data.isShooting(hand) && decision != EnumFireDecision.SHOOT)
        {
            data.setShooting(hand, false);
            PacketHandler.sendToDimension(level.dimension(), new PacketGunShootClient(player.getUUID(), hand, false));
        }

        if (soundDelay <= 0 && StringUtils.isNotBlank(configType.getIdleSound()))
        {
            PacketPlaySound.sendSoundPacket(player, configType.getIdleSoundRange(), configType.getIdleSound(), false);
            soundDelay = configType.getIdleSoundLength();
        }

        if (soundDelay > 0)
            soundDelay--;
    }

    private void ensureGunTags(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();

        // Create tag if missing or empty
        if (tag == null || tag.isEmpty())
        {
            CompoundTag newTag = new CompoundTag();
            newTag.putString(NBT_PAINT, configType.getDefaultPaintjob().getIconName());

            ListTag ammoList = new ListTag();
            for (int j = 0; j < configType.getNumAmmoItemsInGun(stack); j++)
                ammoList.add(new CompoundTag());

            newTag.put(NBT_AMMO, ammoList);
            stack.setTag(newTag);
            tag = newTag;
        }

        // Repair missing keys
        if (!tag.contains(NBT_AMMO, Tag.TAG_LIST) || !tag.contains(NBT_PAINT, Tag.TAG_STRING))
        {
            ListTag ammoList = new ListTag();
            for (int j = 0; j < configType.getNumAmmoItemsInGun(stack); j++)
                ammoList.add(new CompoundTag());

            tag.put(NBT_AMMO, ammoList);
            tag.putString(NBT_PAINT, configType.getDefaultPaintjob().getIconName());
            configType.checkForTags(stack);
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

    public List<ItemStack> getBulletItemStackList(ItemStack gun)
    {
        return IntStream.range(0, configType.getNumAmmoItemsInGun(gun))
            .mapToObj(i -> getBulletItemStack(gun, i))
            .filter(s -> s != null && s.getItem() instanceof ShootableItem)
            .toList();
    }

    public int getReloadCount(ItemStack gunStack)
    {
        int maxAmmo = configType.getNumAmmoItemsInGun(gunStack);
        return (maxAmmo <= 1) ? 1 : Math.toIntExact(getBulletItemStackList(gunStack).stream()
            .filter(stack -> (gunStack.getMaxDamage() - gunStack.getDamageValue()) == 0)
            .count());
    }

    public float getActualReloadTime(ItemStack gunStack)
    {
        //TODO: implement Enchantments
        //reloadTime = EnchantmentModule.ModifyReloadTime(reloadTime, player, otherHand);
        int maxAmmo = configType.getNumAmmoItemsInGun(gunStack);
        return (maxAmmo <= 1) ? configType.getReloadTime(gunStack) : (configType.getReloadTime(gunStack) / maxAmmo) * getReloadCount(gunStack);
    }

    public void setPreferredAmmo(ItemStack gun, String ammoName)
    {
        CompoundTag tag = gun.getOrCreateTag();
        tag.putString(NBT_PREFERRED_AMMO, ammoName);
    }

    public String getPreferredAmmo(ItemStack gun)
    {
        CompoundTag tag = gun.getOrCreateTag();
        if (!tag.contains(NBT_PREFERRED_AMMO))
            setPreferredAmmo(gun, configType.getAmmo().iterator().next());

        return tag.getString(NBT_PREFERRED_AMMO);
    }
}
