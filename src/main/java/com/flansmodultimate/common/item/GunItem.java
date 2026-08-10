package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.enchantments.EnchantmentModule;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.guns.EnumFireDecision;
import com.flansmodultimate.common.guns.EnumFunction;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunShootClient;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.util.ModUtils;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class GunItem extends Item implements IPaintableItem<GunType>, ICustomRendereredItem<GunType>
{
    public static final int LOCK_ON_SOUND_RANGE = 10;

    public static final String NBT_AMMO = "ammo";
    public static final String NBT_SECONDARY_AMMO = "secondary_ammo";
    public static final String NBT_PREFERRED_AMMO = "preferred_ammo";
    public static final String NBT_LEGENDARY_CRAFTER = "legendary_crafter";
    public static final String NBT_ENTITY_LOCK_ON = "lock_on";
    public static final String NBT_ATTACHMENTS = "attachments";
    public static final String NBT_GENERIC = "generic_";
    public static final String NBT_BARREL = "barrel";
    public static final String NBT_SCOPE = "scope";
    public static final String NBT_STOCK = "stock";
    public static final String NBT_GRIP = "grip";
    public static final String NBT_GADGET = "gadget";
    public static final String NBT_SLIDE = "slide";
    public static final String NBT_PUMP = "pump";
    public static final String NBT_ACCESSORY = "accessory";
    public static final String NBT_SECONDARY_FIRE = "secondary_fire";
    public static final String NBT_GUN_MODE = "gun_mode";
    public static final String NBT_KNOCKBACK_RESISTANCE_UUID = "knockback_resistance_uuid";
    public static final String NBT_MOVEMENT_SPEED_UUID = "movement_speed_uuid";
    public static final String NBT_ATTACK_DAMAGE_UUID = "attack_damage_uuid";

    @Getter
    protected final GunType configType;
    protected final String shortname;
    @Getter
    protected final GunItemHandler gunItemHandler;
    protected String originGunbox;
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
    public boolean useCustomRendererInHand()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererOnGround()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererInFrame()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererInGui()
    {
        return false;
    }

    public boolean useAimingAnimation()
    {
        return true;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity)
    {
        return 100;
    }

    @Override
    @NotNull
    public UseAnim getUseAnimation(@NotNull ItemStack stack)
    {
        return configType.getItemUseAction();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);

        // Legendary crafter tag
        CompoundTag customTag = ItemStackData.copy(stack);
        if (customTag.contains(NBT_LEGENDARY_CRAFTER, Tag.TAG_STRING))
        {
            String crafter = customTag.getString(NBT_LEGENDARY_CRAFTER);
            tooltipComponents.add(Component.literal("Legendary Skin Crafted by " + crafter).withStyle(ChatFormatting.GOLD));
        }

        if (configType.isDeployable())
            tooltipComponents.add(Component.literal("[Deployable]").withStyle(ChatFormatting.YELLOW));

        if (!ClientHooks.TOOLTIPS.isShiftDown())
        {
            // Attachments
            if (configType.isShowAttachments())
            {
                List<ItemStack> attachmentItems = configType.getCurrentAttachmentItems(stack);

                if (!attachmentItems.isEmpty())
                    tooltipComponents.add(Component.literal("Attachments").withStyle(ChatFormatting.YELLOW));

                for (ItemStack attachmentItem : attachmentItems)
                    tooltipComponents.add(Component.literal(attachmentItem.getDisplayName().getString()).withStyle(ChatFormatting.AQUA));
            }


            // Ammo info
            for (ItemStack bulletStack : getBulletItemStackList(stack, context.registries()))
            {
                if (bulletStack != null && !bulletStack.isEmpty() && bulletStack.getItem() instanceof BulletItem bulletItem)
                {
                    int remaining = ShootableItem.getRoundsRemaining(bulletStack);
                    int max = bulletItem.getConfigType().getRoundsPerItem();
                    String line;
                    if (max > 1)
                    {
                        int stackCount = bulletStack.getCount();
                        if (stackCount > 1)
                        {
                            int totalRounds = ShootableItem.getTotalRounds(bulletStack);
                            line = bulletStack.getDisplayName().getString() + " " + remaining + "/" + max + " (x" + stackCount + " = " + totalRounds + ")";
                        }
                        else
                        {
                            line = bulletStack.getDisplayName().getString() + " " + remaining + "/" + max;
                        }
                    }
                    else
                    {
                        line = bulletStack.getDisplayName().getString() + " x" + bulletStack.getCount();
                    }
                    tooltipComponents.add(Component.literal(line).withStyle(ChatFormatting.DARK_BLUE));
                }
            }

            tooltipComponents.add(Component.empty());

            Component keyName = ClientHooks.TOOLTIPS.getShiftKeyName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
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

            List<ShootableType> ammoTypes = new ArrayList<>(configType.getAmmoTypes());
            getBulletItemStackList(stack, context.registries()).stream()
                .map(ItemStack::getItem)
                .filter(ShootableItem.class::isInstance)
                .map(ShootableItem.class::cast)
                .map(ShootableItem::getConfigType)
                .findFirst()
                .ifPresent(loadedAmmoType ->
                {
                    for (int i = 0; i < ammoTypes.size(); i++)
                    {
                        ShootableType ammoType = ammoTypes.get(i);
                        if (ammoType.getShortName().equals(loadedAmmoType.getShortName()))
                        {
                            ammoTypes.remove(i);
                            ammoTypes.add(0, ammoType);
                            break;
                        }
                    }
                });
            if (configType.isShowDamage() && !ammoTypes.isEmpty())
            {
                tooltipComponents.add(Component.literal("Damage: ").withStyle(ChatFormatting.BLUE));

                if (!ammoTypes.stream().allMatch(ShootableType::useKineticDamageSystem))
                {
                    tooltipComponents.add(Component.literal("  vsLiving").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(" vsPlayer").withStyle(ChatFormatting.RED))
                        .append(Component.literal(" vsVehicle").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" vsPlane").withStyle(ChatFormatting.LIGHT_PURPLE)));
                }

                if (ammoTypes.size() > 10)
                {
                    Map<DamageTooltipValues, List<String>> groupedDamageLines = new LinkedHashMap<>();
                    for (ShootableType shootableType : ammoTypes)
                    {
                        DamageTooltipValues damageValues = getDamageTooltipValues(shootableType, stack);
                        groupedDamageLines.computeIfAbsent(damageValues, ignored -> new ArrayList<>())
                            .add(ModUtils.getItemLocalizedName(shootableType.getShortName()));
                    }

                    groupedDamageLines.forEach((damageValues, ammoNames) -> tooltipComponents.add(createDamageComponent(String.join(", ", ammoNames), damageValues)));
                }
                else
                {
                    for (ShootableType shootableType : ammoTypes)
                        tooltipComponents.add(createDamageComponent(ModUtils.getItemLocalizedName(shootableType.getShortName()), getDamageTooltipValues(shootableType, stack)));
                }
            }

            if (configType.getPrimaryFunction().isMelee() || configType.getSecondaryFunction().isMelee())
                tooltipComponents.add(IFlanItem.statLine("Melee Damage", IFlanItem.formatFloat(configType.getMeleeDamage(stack, false))));

            if (configType.isShowRecoil())
            {
                tooltipComponents.add(IFlanItem.statLine("Vertical Recoil", IFlanItem.formatFloat(configType.getDisplayVerticalRecoil(stack))));
                tooltipComponents.add(IFlanItem.statLine("Horizontal Recoil", IFlanItem.formatFloat(configType.getDisplayHorizontalRecoil(stack))));

                String sprintingControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, true, false));
                String sneakingControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, false, true));
                String normalControl = IFlanItem.formatFloat(1F - configType.getRecoilControl(stack, false, false));

                tooltipComponents.add(Component.literal("Recoil Control: ").withStyle(ChatFormatting.BLUE));
                tooltipComponents.add(Component.literal("  sprinting").withStyle(ChatFormatting.RED)
                    .append(Component.literal(" crouching").withStyle(ChatFormatting.GREEN)));
                tooltipComponents.add(Component.literal("  " + sprintingControl).withStyle(ChatFormatting.RED)
                    .append(Component.literal(" " + normalControl).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" " + sneakingControl).withStyle(ChatFormatting.GREEN)));
            }

            if (configType.isShowSpread())
                tooltipComponents.add(IFlanItem.statLine("Dispersion", IFlanItem.formatFloat(configType.getDispersionForDisplay(stack)) + "°"));

            if (configType.getSwitchDelay() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Switch Delay", IFlanItem.formatFloat(configType.getSwitchDelay())));

            if (configType.isShowReloadTime())
                tooltipComponents.add(IFlanItem.statLine("Reload Time", IFlanItem.formatFloat(configType.getReloadTime(stack) / 20F) + "s"));

            if (configType.isShowBulletSpeed()) {
                float bulletSpeed = configType.getBulletSpeed(stack);
                tooltipComponents.add(IFlanItem.statLine("Muzzle Velocity", (bulletSpeed != 0F) ? (IFlanItem.formatFloat(bulletSpeed * 20F) + "m/s") : "∞"));
            }

            if (configType.isShowShootDelay())
                tooltipComponents.add(IFlanItem.statLine("Fire Rate", IFlanItem.formatFloat(1200F / configType.getShootDelay(stack)) + "rpm"));

            if (configType.isShowMode())
                tooltipComponents.add(IFlanItem.statLine("Mode", configType.getFireMode(stack).name().toLowerCase()));

            if (configType.getKnockback() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Shooter Knockback", IFlanItem.formatFloat(configType.getKnockback())));

            float zoomFactor = Math.max(configType.getCurrentScope(stack).getZoomFactor(), configType.getCurrentScope(stack).getFovFactor());
            if (zoomFactor != 1F)
                tooltipComponents.add(IFlanItem.statLine("Zoom Factor", "x" + IFlanItem.formatFloat(zoomFactor)));
        }
    }

    private DamageTooltipValues getDamageTooltipValues(ShootableType shootableType, ItemStack stack)
    {
        if (shootableType.useKineticDamageSystem())
            return new DamageTooltipValues(true, IFlanItem.formatFloat(configType.getDamageForDisplay(shootableType, stack), 1), false, "", false, "", false, "", false, "");

        float damage = configType.getDamageForDisplay(shootableType, stack);
        float damageVsLiving = configType.getDamageForDisplay(shootableType, stack, LivingEntity.class);
        float damageVsPlayer = configType.getDamageForDisplay(shootableType, stack, Player.class);
        float damageVsVehicle = configType.getDamageForDisplay(shootableType, stack, Vehicle.class);
        float damageVsPlane = configType.getDamageForDisplay(shootableType, stack, Plane.class);
        final float EPS = 0.0001F;

        boolean showLiving = shootableType.getDamage().isReadDamageVsLiving() && Math.abs(damage - damageVsLiving) > EPS;
        boolean showPlayer = shootableType.getDamage().isReadDamageVsPlayer() && Math.abs(damageVsPlayer - damageVsLiving) > EPS;
        boolean showVehicle = shootableType.getDamage().isReadDamageVsVehicles() && Math.abs(damageVsVehicle - damage) > EPS;
        boolean showPlane = shootableType.getDamage().isReadDamageVsPlanes() && Math.abs(damageVsPlane - damageVsVehicle) > EPS;

        return new DamageTooltipValues(
            false,
            IFlanItem.formatFloat(damage, 1),
            showLiving, showLiving ? IFlanItem.formatFloat(damageVsLiving, 1) : "",
            showPlayer, showPlayer ? IFlanItem.formatFloat(damageVsPlayer, 1) : "",
            showVehicle, showVehicle ? IFlanItem.formatFloat(damageVsVehicle, 1) : "",
            showPlane, showPlane ? IFlanItem.formatFloat(damageVsPlane, 1) : ""
        );
    }

    private static MutableComponent createDamageComponent(String label, DamageTooltipValues damageValues)
    {
        MutableComponent damageComponent = IFlanItem.indentedStatLine(label, damageValues.damage());

        if (damageValues.showLiving())
            damageComponent.append(Component.literal(" " + damageValues.damageVsLiving()).withStyle(ChatFormatting.GREEN));
        if (damageValues.showPlayer())
            damageComponent.append(Component.literal(" " + damageValues.damageVsPlayer()).withStyle(ChatFormatting.RED));
        if (damageValues.showVehicle())
            damageComponent.append(Component.literal(" " + damageValues.damageVsVehicle()).withStyle(ChatFormatting.AQUA));
        if (damageValues.showPlane())
            damageComponent.append(Component.literal(" " + damageValues.damageVsPlane()).withStyle(ChatFormatting.LIGHT_PURPLE));

        return damageComponent;
    }

    private record DamageTooltipValues(
        boolean kinetic, String damage,
        boolean showLiving, String damageVsLiving,
        boolean showPlayer, String damageVsPlayer,
        boolean showVehicle, String damageVsVehicle,
        boolean showPlane, String damageVsPlane
    ) {}

    @Override
    public PaintableType getPaintableType()
    {
        return configType;
    }

    /**
     * Called when the player attacks an entity with this item.
     * Return true to cancel the hit (no damage), false to allow normal attack.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target)
    {
        return configType.getPrimaryFunction() != EnumFunction.MELEE && configType.getSecondaryFunction() != EnumFunction.MELEE;
    }

    /**
     * Called when the player starts breaking a block with this item.
     * Return true to cancel further processing (and client-side breaking animation).
     * We bounce a block update on the server to ensure proper visuals in creative.
     */
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
        ItemStack stack = player.getItemInHand(hand);

        if (configType.isDeployable() && gunItemHandler.tryPlaceDeployable(level, player, stack))
            return InteractionResultHolder.sidedSuccess(stack, false);

        boolean dualWield = player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof GunItem
            && player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof GunItem;

        if (!dualWield)
        {
            boolean canZoom = configType.getSecondaryFunction().isZoom() || configType.getPrimaryFunction().isZoom()
                || configType.getZoomFactor() > 1F || configType.getFovFactor() > 1F;

            if (!canZoom)
                ClientHooks.PLAYER.swingIfLocalPlayer(player, hand);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack)
    {
        if (stack.getEquipmentSlot() != EquipmentSlot.MAINHAND)
            return super.getDefaultAttributeModifiers(stack);

        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : super.getDefaultAttributeModifiers(stack).modifiers())
            b.add(entry.attribute(), entry.modifier(), entry.slot());
        b.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "knockback_resistance"), configType.getKnockbackModifier(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        b.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "movement_speed"), configType.getMovementSpeed(stack) - 1F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
        b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "attack_damage"), configType.getMeleeDamage(stack, false), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return b.build();
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand)
    {
        if (StringUtils.isNotBlank(configType.getMeleeSound()))
            PacketPlaySound.sendSoundPacket(entity, configType.getMeleeSoundRange(), configType.getMeleeSound(), true);
        return false;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player)
    {
        if (level instanceof Level lvl)
        {
            BlockState state = lvl.getBlockState(pos);
            return state.getMenuProvider(lvl, pos) != null;
        }
        return false;
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
            ClientHooks.GUN.tickGunItem(this, level, player, data, stack, hand, dualWield);
        else
            serverTick(level, (ServerPlayer) player, data, stack, hand);

        gunItemHandler.handleMinigunEffects(level, player, data, hand);
        gunItemHandler.checkForLockOn(level, player, data, hand);
        gunItemHandler.checkForMelee(level, player, data, stack);
    }

    protected void serverTick(Level level, @NotNull ServerPlayer player, @NotNull PlayerData data, ItemStack gunStack, InteractionHand hand)
    {
        ensureGunTags(gunStack);

        if (configType.getPrimaryFunction() == EnumFunction.SHOOT)
        {
            EnumFireDecision decision = gunItemHandler.computeFireDecision(data, gunStack, hand, level.registryAccess());
            if (decision == EnumFireDecision.RELOAD)
            {
                boolean reloading = gunItemHandler.doPlayerReload(level, player, data, gunStack, hand, false);
                if (!reloading)
                    gunItemHandler.playEmptyClick(player, data, gunStack, hand);
            }
            else if (decision == EnumFireDecision.SHOOT)
                gunItemHandler.doPlayerShoot(level, player, data, gunStack, hand);

            // Stop shooting
            if (data.isShooting(hand) && decision != EnumFireDecision.SHOOT)
            {
                data.setShooting(hand, false);
                PacketHandler.sendToDimension(level.dimension(), new PacketGunShootClient(player.getUUID(), hand, false));
            }
        }
        else if (configType.getPrimaryFunction() == EnumFunction.CUSTOM_MELEE && data.isShootKeyPressed(hand))
            gunItemHandler.doCustomMelee(level, player, data, hand);

        if (configType.getSecondaryFunction() == EnumFunction.CUSTOM_MELEE && data.isSecondaryFunctionKeyPressed())
            gunItemHandler.doCustomMelee(level, player, data, hand);

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
        CompoundTag tag = ItemStackData.copy(stack);
        boolean dirty = false;

        if (!tag.contains(NBT_AMMO, Tag.TAG_LIST))
        {
            ListTag ammoList = new ListTag();
            for (int j = 0; j < configType.getNumAmmoItemsInGun(stack); j++)
                ammoList.add(new CompoundTag());

            tag.put(NBT_AMMO, ammoList);
            dirty = true;
        }

        if (!tag.contains(IPaintableItem.NBT_PAINTJOB_ID, Tag.TAG_INT))
        {
            tag.putInt(NBT_PAINTJOB_ID, configType.getDefaultPaintjob().getId());
            dirty = true;
        }

        if (dirty)
            ItemStackData.set(stack, tag);

        configType.checkForTags(stack);
    }

    /**
     * Get the ammo item stack stored in the gun's NBT data (the loaded magazine / bullets).
     * @param id: some guns use multiple bullet items instead of one magazine, id is here the index to identify which one.
     */
    public ItemStack getAmmoItemStack(ItemStack gun, int id, HolderLookup.Provider registries) {
        if (gun.isEmpty())
            return ItemStack.EMPTY;

        CompoundTag tag = ItemStackData.copy(gun);

        String nbt = configType.getSecondaryFire(gun) ? NBT_SECONDARY_AMMO : NBT_AMMO;

        if (!tag.contains(nbt, Tag.TAG_LIST))
        {
            ListTag list = new ListTag();
            for (int i = 0; i < configType.getNumAmmoItemsInGun(gun); i++)
                list.add(new CompoundTag());
            return ItemStack.EMPTY;
        }

        ListTag list = tag.getList(nbt, Tag.TAG_COMPOUND);
        if (id < 0 || id >= list.size())
            return ItemStack.EMPTY;

        CompoundTag slotTag = list.getCompound(id);
        return ItemStackData.parse(registries, slotTag);
    }

    /**
     * Set the bullet item stack stored in the gun's NBT data (the loaded magazine / bullets).
     * @param id: some guns use multiple bullet items instead of one magazine, id is here the index to identify which one.
     */
    public void setBulletItemStack(ItemStack gun, ItemStack bullet, int id, HolderLookup.Provider registries) {
        if (gun.isEmpty() || id < 0)
            return;

        ListTag list;
        CompoundTag tag = ItemStackData.copy(gun);
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

        CompoundTag slotTag = (bullet == null || bullet.isEmpty()) ? new CompoundTag() : ItemStackData.save(bullet, registries);

        list.set(id, slotTag);
        tag.put(nbt, list);
        ItemStackData.set(gun, tag);
    }

    @Unmodifiable
    public List<ItemStack> getBulletItemStackList(ItemStack gun, HolderLookup.Provider registries)
    {
        return IntStream.range(0, configType.getNumAmmoItemsInGun(gun))
            .mapToObj(i -> getAmmoItemStack(gun, i, registries))
            .filter(s -> s != null && s.getItem() instanceof ShootableItem && ShootableItem.hasRoundsLeft(s))
            .toList();
    }

    public int getReloadCount(ItemStack gunStack, HolderLookup.Provider registries)
    {
        int maxAmmo = configType.getNumAmmoItemsInGun(gunStack);
        if (maxAmmo <= 1)
            return 1;
        int emptySlots = 0;
        for (int i = 0; i < maxAmmo; i++)
        {
            ItemStack bulletStack = getAmmoItemStack(gunStack, i, registries);
            if (bulletStack == null || bulletStack.isEmpty() || !ShootableItem.hasRoundsLeft(bulletStack))
                emptySlots++;
        }
        return emptySlots;
    }

    public float getActualReloadTime(ItemStack gunStack, HolderLookup.Provider registries, @Nullable ItemStack otherHand)
    {
        int maxAmmo = configType.getNumAmmoItemsInGun(gunStack);
        float reloadTime = (maxAmmo <= 1) ? configType.getReloadTime(gunStack) : (configType.getReloadTime(gunStack) / maxAmmo) * getReloadCount(gunStack, registries);
        return EnchantmentModule.getModifiedReloadTime(reloadTime, otherHand);
    }

    public void setPreferredAmmo(ItemStack gun, String ammoName)
    {
        ItemStackData.update(gun, tag -> tag.putString(NBT_PREFERRED_AMMO, ammoName));
    }

    public String getPreferredAmmo(ItemStack gun)
    {
        CompoundTag tag = ItemStackData.copy(gun);
        if (!tag.contains(NBT_PREFERRED_AMMO))
            setPreferredAmmo(gun, configType.getAmmo().iterator().next());

        return tag.getString(NBT_PREFERRED_AMMO);
    }
}
