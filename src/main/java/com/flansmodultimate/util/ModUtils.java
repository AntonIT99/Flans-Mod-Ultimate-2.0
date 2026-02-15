package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.EnumMovement;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModUtils
{
    public static boolean isVehicleLike(Entity entity)
    {
        if (entity.getClass().getName().toLowerCase(Locale.ROOT).contains("vehicle"))
            return true;

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null)
            return false;

        String path = id.getPath().toLowerCase(Locale.ROOT);
        return path.contains("vehicle");
    }

    public static boolean isPlaneLike(Entity entity)
    {
        if (entity.getClass().getName().toLowerCase(Locale.ROOT).contains("plane"))
            return true;

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null)
            return false;

        String path = id.getPath().toLowerCase(Locale.ROOT);
        return path.contains("plane");
    }

    public static boolean hasGunItemInHands(LivingEntity living)
    {
        return living.getMainHandItem().getItem() instanceof GunItem || living.getOffhandItem().getItem() instanceof GunItem;
    }

    public static List<GunItem> getGunItemsInHands(LivingEntity living)
    {
        List<GunItem> list = new ArrayList<>();
        if (living.getMainHandItem().getItem() instanceof GunItem gunItem)
            list.add(gunItem);
        if (living.getOffhandItem().getItem() instanceof GunItem gunItem)
            list.add(gunItem);
        return list;
    }

    //TODO: add exceptions for other entities of this mod that should not be hit by bullets
    public static boolean canEntityBeHitByBullets(Entity entity)
    {
        return !(entity instanceof Bullet)
            && !(entity instanceof ItemEntity)
            && !(entity instanceof Projectile)
            && !(entity instanceof ExperienceOrb)
            && !(entity instanceof Display)
            && !(entity instanceof Interaction);
    }

    public static List<Entity> queryEntities(Level level, @Nullable Entity except, AABB box, @Nullable Predicate<? super Entity> filter)
    {
        return level.getEntities(except, box, e -> e != null && e.isAlive() && (except == null || e != except) && (filter == null || filter.test(e)));
    }

    public static List<Entity> queryEntities(Level level, @Nullable Entity except, AABB box)
    {
        return queryEntities(level, except, box, null);
    }

    public static <T extends Entity> List<T> queryEntities(Level level, @Nullable Entity except, AABB box, Class<T> type, @Nullable Predicate<? super T> filter)
    {
        return level.getEntitiesOfClass(type, box, e -> e != null && e.isAlive() && (except == null || e != except) && (filter == null || filter.test(e)));
    }

    public static List<Entity> queryEntitiesInRange(Level level, Entity source, double radius, @Nullable Predicate<? super Entity> filter)
    {
        AABB box = source.getBoundingBox().inflate(radius);
        return queryEntities(level, source, box, filter);
    }

    public static <T extends Entity> List<T> queryEntitiesInRange(Level level, Entity source, double radius, Class<T> type, @Nullable Predicate<? super T> filter)
    {
        AABB box = source.getBoundingBox().inflate(radius);
        return queryEntities(level, source, box, type, filter);
    }

    public static List<LivingEntity> queryLivingEntities(Level level, @Nullable Entity except, AABB box, @Nullable Predicate<? super LivingEntity> filter)
    {
        return queryEntities(level, except, box, LivingEntity.class, filter);
    }

    public static List<LivingEntity> queryLivingEntities(Level level, AABB box, @Nullable Predicate<? super LivingEntity> filter)
    {
        return queryEntities(level, null, box, LivingEntity.class, filter);
    }

    public static List<LivingEntity> queryLivingEntities(Level level, @Nullable Entity except, AABB box)
    {
        return queryEntities(level, except, box, LivingEntity.class, null);
    }

    public static List<LivingEntity> queryLivingEntities(Level level, AABB box)
    {
        return queryEntities(level, null, box, LivingEntity.class, null);
    }

    public static Optional<ItemStack> getItemStack(@Nullable InfoType infoType)
    {
        return getItemStack(infoType, 1);
    }

    public static Optional<ItemStack> getItemStack(@Nullable InfoType infoType, int amount)
    {
        return getItemStack(infoType, amount, 0);
    }

    public static Optional<ItemStack> getItemStack(@Nullable InfoType infoType, int amount, int damage)
    {
        Optional<Item> item = getItem(infoType);
        if (item.isPresent())
        {
            ItemStack stack = new ItemStack(item.get(), amount);
            if (damage > 0)
                stack.setDamageValue(damage);
            return Optional.of(stack);
        }
        return Optional.empty();
    }

    public static Optional<Item> getItem(@Nullable InfoType infoType)
    {
        if (infoType != null && infoType.getType().isHasItem())
        {
            return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, infoType.getShortName())));
        }
        return Optional.empty();
    }

    /**
     * get an ItemStack from id "modid:name"
     */
    public static Optional<ItemStack> getItemStack(@Nullable String id, int amount, int damage)
    {
        Optional<Item> item = getItemById(id);
        if (item.isPresent())
        {
            ItemStack stack = new ItemStack(item.get(), amount);
            if (damage > 0)
                stack.setDamageValue(damage);
            return Optional.of(stack);
        }
        return Optional.empty();
    }

    /**
     * get an Item from id "modid:name"
     */
    public static Optional<Item> getItemById(@Nullable String id)
    {
        if (StringUtils.isBlank(id))
            return Optional.empty();

        id = ResourceUtils.sanitize(id.trim());

        // If no namespace, assume minecraft
        if (!id.contains(":"))
            id = "minecraft:" + id;

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null)
            return Optional.empty();

        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(rl));
    }

    private static boolean isInteger(String s)
    {
        if (s == null)
            return false;
        try
        {
            Integer.parseInt(s);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    public static Optional<BlockState> getBlockState(String id)
    {
        //Warning: Block.stateById() probably not working correctly in 1.20+ -> TODO: use a mapping from 1.12?
        if (isInteger(id))
        {
            return Optional.of(Block.stateById(Integer.parseInt(id)));
        }
        else
        {
            return Optional.ofNullable(ResourceLocation.tryParse(id)).map(ForgeRegistries.BLOCKS::getValue).map(Block::defaultBlockState);
        }
    }

    public static Optional<ItemStack> getItemStack(String id)
    {
        //Warning: Item.byId() probably not working correctly in 1.20+ -> TODO: use a mapping from 1.12?
        if (isInteger(id))
        {
            return Optional.of(new ItemStack(Item.byId(Integer.parseInt(id))));
        }
        else
        {
            return Optional.ofNullable(ResourceLocation.tryParse(id)).map(ForgeRegistries.ITEMS::getValue).map(ItemStack::new);
        }
    }

    public static boolean isGlass(BlockState state)
    {
        return state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES) || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE);
    }

    public static void destroyBlock(ServerLevel level, BlockPos pos, @Nullable Entity cause, boolean dropBlock)
    {
        Player player;

        if (cause instanceof Player p)
        {
            // real player
            player = p;
        }
        else
        {
            // fake player (Forge helper)
            GameProfile profile = new GameProfile(UUID.randomUUID(), "fakePlayer");
            player = FakePlayerFactory.get(level, profile);
        }

        BlockState state = level.getBlockState(pos);

        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        MinecraftForge.EVENT_BUS.post(breakEvent);

        if (breakEvent.isCanceled())
            return;

        level.destroyBlock(pos, dropBlock);
    }

    /**
     * Method for dropping Flan's Mod items at the entity's location
     */
    public static void dropItem(Level level, Entity entity, @Nullable String itemName, IContentProvider contentPack)
    {
        if (!level.isClientSide && StringUtils.isNotBlank(itemName))
        {
            ItemStack dropStack = InfoType.getRecipeElement(itemName, contentPack);
            entity.spawnAtLocation(dropStack, 0.5F);
        }
    }

    public static String getItemLocalizedName(String itemId)
    {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, itemId));
        if (item != null)
            return item.getDescription().getString();
        return itemId;
    }

    public static float getYawFromDirection(Vec3 dir)
    {
        if (dir.lengthSqr() < 1e-12)
            return 0.0F;

        return Mth.wrapDegrees((float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG));
    }

    public static float getPitchFromDirection(Vec3 dir)
    {
        if (dir.lengthSqr() < 1e-12)
            return 0.0F;

        return (float) (Mth.atan2(-dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * Mth.RAD_TO_DEG);
    }

    public static Vec3 getDirectionFromPitchAndYaw(float pitchDeg, float yawDeg)
    {
        float yawRad = yawDeg * Mth.DEG_TO_RAD;
        float pitchRad = pitchDeg * Mth.DEG_TO_RAD;

        double x = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double y = -Mth.sin(pitchRad);
        double z = Mth.cos(yawRad) * Mth.cos(pitchRad);

        return new Vec3(x, y, z);
    }

    public static EnumMovement getEnumMovement(LivingEntity entity) {
        if (entity != null) {
            if (entity.isSprinting()) {
                return EnumMovement.SPRINTING;
            }

            Vec3 prevPos = CommonEventHandler.getPrevPos(entity);
            System.out.println(prevPos.x + ", " + entity.getX());
            boolean isMoving = prevPos.x != entity.getX() || prevPos.z != entity.getZ();
            if (isMoving) {
                return EnumMovement.WALKING;
            }

            if (entity.isCrouching()) {
                return EnumMovement.SNEAKING;
            }
        }

        return EnumMovement.NONE;
    }
}
