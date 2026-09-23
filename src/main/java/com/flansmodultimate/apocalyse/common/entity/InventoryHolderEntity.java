package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.platform.item.ItemStackData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The stand-in a player leaves behind when the apocalypse takes them.
 *
 * <p>It keeps the belongings they were carrying, wanders the spot they vanished from, and
 * hands everything back when it is put down — so returning from the apocalypse means
 * finding, and beating, the shape you left behind.</p>
 */
public class InventoryHolderEntity extends PathfinderMob
{
    private static final String NBT_HELD_ITEMS = "held_items";

    private final NonNullList<ItemStack> heldItems = NonNullList.create();

    public InventoryHolderEntity(EntityType<? extends InventoryHolderEntity> type, Level level)
    {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.FOLLOW_RANGE, 24.0D)
            .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    /** Creates a stand-in holding a copy of everything {@code player} is carrying. */
    @Nullable
    public static InventoryHolderEntity createFor(Player player)
    {
        InventoryHolderEntity holder = ApocalypseContent.inventoryHolder.get().create(player.level());
        if (holder == null)
            return null;
        holder.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        // Naming it after its owner is how anyone finds the right one again.
        holder.setCustomName(player.getName());
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty())
                holder.heldItems.add(stack.copy());
        }
        return holder;
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull ServerLevel level, @NotNull DamageSource source, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (level().isClientSide)
            return;
        for (ItemStack stack : heldItems)
            Containers.dropItemStack(level(), getX(), getY(), getZ(), stack.copy());
        heldItems.clear();
    }

    /** The belongings are the point of this entity: it must never quietly vanish. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence()
    {
        return true;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        heldItems.clear();
        ListTag list = tag.getList(NBT_HELD_ITEMS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++)
        {
            ItemStack stack = ItemStackData.parse(level().registryAccess(), list.getCompound(index));
            if (!stack.isEmpty())
                heldItems.add(stack);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (ItemStack stack : heldItems)
            list.add(ItemStackData.save(stack, level().registryAccess()));
        tag.put(NBT_HELD_ITEMS, list);
    }
}
