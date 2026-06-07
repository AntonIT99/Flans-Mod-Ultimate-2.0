package com.flansmodultimate.common.recipe;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.ModUtils;
import com.flansmodultimate.util.ResourceUtils;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RecipeResolver
{
    /**
     * Resolves a legacy recipe token to an item stack with a default count of one.
     * <p>
     * The token may include a legacy metadata suffix such as {@code dyePowder.1}; that suffix is parsed before
     * delegating to {@link #resolve(String, int, int, IContentProvider)}.
     *
     * @param token    raw recipe token from a content pack recipe definition
     * @param provider content pack context used for short name alias lookup
     * @return the resolved stack, or {@link ItemStack#EMPTY} when the token cannot be resolved
     */
    public static ItemStack resolve(String token, @Nullable IContentProvider provider)
    {
        if (StringUtils.isBlank(token))
            return ItemStack.EMPTY;

        return RecipeIngredient.parse(token.trim(), 1, provider).resolve();
    }

    /**
     * Resolves a recipe item id to an item stack for runtime recipe displays and legacy recipe consumers.
     * <p>
     * Resolution order is:
     * namespaced raw registered item id, content-pack short name alias as {@code flansmod:<alias>},
     * registered {@code flansmod:<id>}, registered {@code minecraft:<id>}, legacy item mapping, then vanilla registry
     * path fallback.
     *
     * @param id       raw item id or legacy recipe token
     * @param amount   requested stack size
     * @param damage   legacy metadata value parsed from tokens like {@code cloth.14}
     * @param provider content pack context used for short name alias lookup
     * @return the resolved stack, or {@link ItemStack#EMPTY} when no registered item can be found
     */
    public static ItemStack resolve(String id, int amount, int damage, @Nullable IContentProvider provider)
    {
        if (StringUtils.isBlank(id) || amount <= 0)
            return ItemStack.EMPTY;

        String rawId = id.trim();
        Optional<ItemStack> stack = resolveRecipeItem(rawId, damage, provider, stackResolution(amount, damage));
        if (stack.isPresent())
            return stack.get();

        FlansMod.log.warn("Could not find {} in recipe", rawId);
        return ItemStack.EMPTY;
    }

    /**
     * Resolves a legacy recipe token to a recipe JSON item id.
     * <p>
     * The token may include a legacy metadata suffix such as {@code dyePowder.1}; that suffix is parsed before
     * delegating to {@link #resolveItemId(String, int, IContentProvider)}.
     *
     * @param token    raw recipe token from a content pack recipe definition
     * @param provider content pack context used for short name alias lookup
     * @return the resolved item id, or {@link Optional#empty()} when the token cannot be resolved
     */
    public static Optional<ResourceLocation> resolveItemId(String token, @Nullable IContentProvider provider)
    {
        if (StringUtils.isBlank(token))
            return Optional.empty();

        RecipeIngredient ingredient = RecipeIngredient.parse(token.trim(), 1, provider);
        return resolveItemId(ingredient.getItemName(), ingredient.getMeta(), provider);
    }

    /**
     * Resolves a recipe item id to the item id that should be written into generated recipe JSON.
     * <p>
     * Resolution order is:
     * namespaced raw registered item id, content-pack short name alias as {@code flansmod:<alias>},
     * registered {@code flansmod:<id>}, registered {@code minecraft:<id>}, legacy item mapping, then vanilla registry
     * path fallback.
     *
     * @param id       raw item id or legacy recipe token
     * @param damage   legacy metadata value parsed from tokens like {@code wool.11}
     * @param provider content pack context used for short name alias lookup
     * @return the resolved item id, or {@link Optional#empty()} when no known id can be found
     */
    public static Optional<ResourceLocation> resolveItemId(String id, int damage, @Nullable IContentProvider provider)
    {
        if (StringUtils.isBlank(id))
            return Optional.empty();

        String rawId = id.trim();
        return resolveRecipeItem(rawId, damage, provider, ITEM_ID_RESOLUTION);
    }

    /**
     * Creates the fallback item id used by generated recipe JSON when no registered or legacy item id can be found.
     * <p>
     * This intentionally does not check the item registry. It assumes unresolved content-pack tokens refer to future
     * or generated items. Namespaced tokens keep their namespace; unnamespaced tokens are written as
     * {@code flansmod:<sanitized token>}.
     *
     * @param token raw recipe token from a content pack recipe definition
     * @return fallback item id for generated recipe JSON
     */
    public static ResourceLocation createFallbackItemId(String token)
    {
        String trimmedToken = token.trim();
        if (trimmedToken.contains(":"))
        {
            String[] split = trimmedToken.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(ResourceUtils.sanitize(split[0]), ResourceUtils.sanitize(split[1]));
        }

        return ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, ResourceUtils.sanitize(trimmedToken));
    }

    private static <T> Optional<T> resolveRecipeItem(String rawId, int damage, @Nullable IContentProvider provider, RecipeItemResolution<T> resolution)
    {
        if (hasNamespace(rawId))
            return resolveNamespacedRecipeItem(rawId, damage, provider, resolution);

        String sanitizedId = ResourceUtils.sanitize(rawId);
        String aliasedId = ContentManager.getShortnameAliasInContentPack(sanitizedId, provider);

        Optional<T> result = resolution.flansmod(aliasedId);
        if (result.isPresent())
            return result;

        if (!aliasedId.equals(sanitizedId))
        {
            result = resolution.flansmod(sanitizedId);
            if (result.isPresent())
                return result;
        }

        result = resolution.registered("minecraft", sanitizedId);
        if (result.isPresent())
            return result;

        result = resolution.legacy(sanitizedId, damage);
        if (result.isPresent())
            return result;

        return resolution.vanillaPath(sanitizedId);
    }

    private static <T> Optional<T> resolveNamespacedRecipeItem(String rawId, int damage, @Nullable IContentProvider provider, RecipeItemResolution<T> resolution)
    {
        Optional<T> result = resolution.raw(rawId);
        if (result.isPresent())
            return result;

        String[] split = rawId.split(":", 2);
        String namespace = ResourceUtils.sanitize(split[0]);
        String path = ResourceUtils.sanitize(split[1]);
        if (namespace.equals(FlansMod.FLANSMOD_ID))
        {
            String aliasedId = ContentManager.getShortnameAliasInContentPack(path, provider);
            result = resolution.flansmod(aliasedId);
            if (result.isPresent())
                return result;

            if (!aliasedId.equals(path))
                return resolution.flansmod(path);

            return Optional.empty();
        }

        if (namespace.equals("minecraft"))
        {
            result = resolution.registered("minecraft", path);
            if (result.isPresent())
                return result;

            result = resolution.legacy(path, damage);
            if (result.isPresent())
                return result;

            return resolution.vanillaPath(path);
        }

        return resolution.registered(namespace, path);
    }

    private static boolean hasNamespace(String id)
    {
        int separator = id.indexOf(':');
        return separator > 0 && separator < id.length() - 1;
    }

    private static Optional<ResourceLocation> resolveRawItemId(String id)
    {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null && ForgeRegistries.ITEMS.containsKey(location))
            return Optional.of(location);

        return Optional.empty();
    }

    private static Optional<ItemStack> resolveRawRecipeElement(String id, int amount, int damage)
    {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !ForgeRegistries.ITEMS.containsKey(location))
            return Optional.empty();

        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item == null)
            return Optional.empty();

        ItemStack stack = new ItemStack(item, amount);
        if (damage > 0)
            stack.setDamageValue(damage);

        return Optional.of(stack);
    }

    private static Optional<ResourceLocation> resolveFlansmodItemId(String id)
    {
        InfoType type = InfoType.getInfoType(id);
        if (type != null && type.getType().isHasItem())
            return Optional.of(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, type.getShortName()));

        return resolveRegisteredItemId(FlansMod.FLANSMOD_ID, id);
    }

    private static Optional<ResourceLocation> resolveRegisteredItemId(String namespace, String path)
    {
        ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
        if (location != null && ForgeRegistries.ITEMS.containsKey(location))
            return Optional.of(location);

        return Optional.empty();
    }

    private static Optional<ItemStack> getFlansmodRecipeElement(String id, int amount, int damage)
    {
        Optional<ItemStack> stack = ModUtils.getItemStack(InfoType.getInfoType(id), amount, damage);
        if (stack.isPresent())
            return stack;

        return getRegisteredRecipeElement(FlansMod.FLANSMOD_ID, id, amount, damage);
    }

    private static Optional<ItemStack> getRegisteredRecipeElement(String namespace, String path, int amount, int damage)
    {
        ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
        if (location == null || !ForgeRegistries.ITEMS.containsKey(location))
            return Optional.empty();

        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item == null)
            return Optional.empty();

        ItemStack stack = new ItemStack(item, amount);
        if (damage > 0)
            stack.setDamageValue(damage);

        return Optional.of(stack);
    }

    private static RecipeItemResolution<ItemStack> stackResolution(int amount, int stackDamage)
    {
        return new RecipeItemResolution<>()
        {
            @Override
            public Optional<ItemStack> raw(String id)
            {
                return resolveRawRecipeElement(id, amount, stackDamage);
            }

            @Override
            public Optional<ItemStack> flansmod(String id)
            {
                return getFlansmodRecipeElement(id, amount, stackDamage);
            }

            @Override
            public Optional<ItemStack> registered(String namespace, String path)
            {
                return getRegisteredRecipeElement(namespace, path, amount, stackDamage);
            }

            @Override
            public Optional<ItemStack> legacy(String id, int damage)
            {
                ItemStack stack = getLegacyRecipeElement(id, amount, damage);
                return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
            }

            @Override
            public Optional<ItemStack> vanillaPath(String id)
            {
                return getVanillaRegistryPathRecipeElement(id, amount);
            }
        };
    }

    private static final RecipeItemResolution<ResourceLocation> ITEM_ID_RESOLUTION = new RecipeItemResolution<>()
    {
        @Override
        public Optional<ResourceLocation> raw(String id)
        {
            return resolveRawItemId(id);
        }

        @Override
        public Optional<ResourceLocation> flansmod(String id)
        {
            return resolveFlansmodItemId(id);
        }

        @Override
        public Optional<ResourceLocation> registered(String namespace, String path)
        {
            return resolveRegisteredItemId(namespace, path);
        }

        @Override
        public Optional<ResourceLocation> legacy(String id, int damage)
        {
            return getLegacyRecipeItemId(id, damage);
        }

        @Override
        public Optional<ResourceLocation> vanillaPath(String id)
        {
            return getVanillaRegistryPathRecipeItemId(id);
        }
    };

    private interface RecipeItemResolution<T>
    {
        Optional<T> raw(String id);

        Optional<T> flansmod(String id);

        Optional<T> registered(String namespace, String path);

        Optional<T> legacy(String id, int damage);

        Optional<T> vanillaPath(String id);
    }

    private static ItemStack getLegacyRecipeElement(String id, int amount, int damage)
    {
        return getLegacyRecipeItem(id, damage)
            .map(item -> stack(item, amount))
            .orElse(ItemStack.EMPTY);
    }

    private static Optional<ResourceLocation> getLegacyRecipeItemId(String id, int damage)
    {
        return getLegacyRecipeItem(id, damage).flatMap(RecipeResolver::id);
    }

    private static Optional<ItemStack> getVanillaRegistryPathRecipeElement(String id, int amount)
    {
        return getVanillaRegistryPathRecipeItem(id).map(item -> new ItemStack(item, amount));
    }

    private static Optional<ResourceLocation> getVanillaRegistryPathRecipeItemId(String id)
    {
        return getVanillaRegistryPathRecipeItem(id).flatMap(RecipeResolver::id);
    }

    private static Optional<ItemLike> getVanillaRegistryPathRecipeItem(String id)
    {
        String lookupPath = getLookupPath(id);
        for (Item item : ForgeRegistries.ITEMS)
        {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId != null && itemId.getNamespace().equals("minecraft") && registryPathMatches(itemId.getPath(), lookupPath))
                return Optional.of(item);
        }
        return Optional.empty();
    }

    private static boolean registryPathMatches(String registryPath, String lookupPath)
    {
        return registryPath.equals(lookupPath) || registryPath.replace("_", StringUtils.EMPTY).equals(lookupPath.replace("_", StringUtils.EMPTY));
    }

    private static String getLookupPath(String id)
    {
        String lookupId = id.trim();
        if (lookupId.contains(":"))
            lookupId = lookupId.split(":", 2)[1];
        return ResourceUtils.sanitize(lookupId);
    }

    private static Optional<ItemLike> getLegacyRecipeItem(String id, int damage)
    {
        ItemLike item = switch (id)
        {
            case "workbench" -> Items.CRAFTING_TABLE;
            case "stone" -> Items.STONE;
            case "skull" -> Items.SKELETON_SKULL;
            case "thinglass" -> Items.GLASS_PANE;
            case "blockcoal" -> Items.COAL_BLOCK;
            case "button" -> Items.STONE_BUTTON;
            case "netherstar" -> Items.NETHER_STAR;
            case "helmetdiamond" -> Items.DIAMOND_HELMET;
            case "beefraw" -> Items.BEEF;
            case "stonebrick" -> Items.STONE_BRICKS;
            case "flintandsteel" -> Items.FLINT_AND_STEEL;
            case "fenceiron" -> Items.IRON_BARS;
            case "rottenflesh" -> Items.ROTTEN_FLESH;
            case "dooriron" -> Items.IRON_DOOR;
            case "doorwood" -> Items.OAK_DOOR;
            case "clayitem" -> Items.CLAY_BALL;
            case "iron_trapdoor" -> Blocks.IRON_TRAPDOOR;
            case "trapdoor" -> Blocks.OAK_TRAPDOOR;
            case "gunpowder", "sulphur" -> Items.GUNPOWDER;
            case "ingotiron", "iron", "ingotsteel", "ingotnickel", "ingotlead", "ingottin" -> Items.IRON_INGOT;
            case "ingotgold", "gold", "ingotelectrum", "ingotconstantan", "ingotsilver", "ingotbronze" -> Items.GOLD_INGOT;
            case "ingotcopper" -> Items.COPPER_INGOT;
            case "nuggetiron", "nuggetsteel", "nuggetnickel", "nuggetlead", "nuggettin", "nuggetcopper" -> Items.IRON_NUGGET;
            case "nuggetgold", "nuggetelectrum", "nuggetconstantan", "nuggetsilver", "nuggetbronze" -> Items.GOLD_NUGGET;
            case "blockiron", "blocksteel", "blocknickel", "blocklead", "blocktin" -> Blocks.IRON_BLOCK;
            case "blockgold", "blockelectrum", "blockconstantan", "blocksilver", "blockbronze" -> Blocks.GOLD_BLOCK;
            case "blockcopper" -> Blocks.COPPER_BLOCK;
            case "blockdiamond" -> Blocks.DIAMOND_BLOCK;
            case "blockemerald" -> Blocks.EMERALD_BLOCK;
            case "blockredstone" -> Blocks.REDSTONE_BLOCK;
            case "boat" -> Items.OAK_BOAT;
            case "log" -> legacyLog(damage);
            case "log2" -> legacyLog2(damage);
            case "wood", "planks", "treatedplanks" -> legacyPlanks(damage);
            case "cloth", "wool" -> legacyWool(damage);
            case "dyepowder" -> legacyDye(damage);
            case "lightgem", "yellowdust", "lightstone" -> Items.GLOWSTONE_DUST;
            case "slimeball" -> Items.SLIME_BALL;
            case "enderpearl" -> Items.ENDER_PEARL;
            case "reeds" -> Items.SUGAR_CANE;
            case "seeds" -> Items.WHEAT_SEEDS;
            case "diode" -> Items.REPEATER;
            case "hellsand" -> Items.SOUL_SAND;
            case "mushroom" -> Items.BROWN_MUSHROOM;
            case "leaves" -> Items.OAK_LEAVES;
            case "bucketlava" -> Items.LAVA_BUCKET;
            case "bucketwater" -> Items.WATER_BUCKET;
            case "pistonbase" -> Items.PISTON;
            default -> null;
        };
        return Optional.ofNullable(item);
    }

    private static ItemStack stack(ItemLike item, int amount)
    {
        return new ItemStack(item, amount);
    }

    private static Optional<ResourceLocation> id(ItemLike item)
    {
        return Optional.ofNullable(ForgeRegistries.ITEMS.getKey(item.asItem()));
    }

    private static ItemLike legacyLog(int damage)
    {
        return switch (damage)
        {
            case 1 -> Blocks.SPRUCE_LOG;
            case 2 -> Blocks.BIRCH_LOG;
            case 3 -> Blocks.JUNGLE_LOG;
            default -> Blocks.OAK_LOG;
        };
    }

    private static ItemLike legacyLog2(int damage)
    {
        return damage == 1 ? Blocks.DARK_OAK_LOG : Blocks.ACACIA_LOG;
    }

    private static ItemLike legacyPlanks(int damage)
    {
        return switch (damage)
        {
            case 1 -> Blocks.SPRUCE_PLANKS;
            case 2 -> Blocks.BIRCH_PLANKS;
            case 3 -> Blocks.JUNGLE_PLANKS;
            case 4 -> Blocks.ACACIA_PLANKS;
            case 5 -> Blocks.DARK_OAK_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private static ItemLike legacyWool(int damage)
    {
        return switch (damage)
        {
            case 1 -> Blocks.ORANGE_WOOL;
            case 2 -> Blocks.MAGENTA_WOOL;
            case 3 -> Blocks.LIGHT_BLUE_WOOL;
            case 4 -> Blocks.YELLOW_WOOL;
            case 5 -> Blocks.LIME_WOOL;
            case 6 -> Blocks.PINK_WOOL;
            case 7 -> Blocks.GRAY_WOOL;
            case 8 -> Blocks.LIGHT_GRAY_WOOL;
            case 9 -> Blocks.CYAN_WOOL;
            case 10 -> Blocks.PURPLE_WOOL;
            case 11 -> Blocks.BLUE_WOOL;
            case 12 -> Blocks.BROWN_WOOL;
            case 13 -> Blocks.GREEN_WOOL;
            case 14 -> Blocks.RED_WOOL;
            case 15 -> Blocks.BLACK_WOOL;
            default -> Blocks.WHITE_WOOL;
        };
    }

    private static ItemLike legacyDye(int damage)
    {
        return switch (damage)
        {
            case 1 -> Items.RED_DYE;
            case 2 -> Items.GREEN_DYE;
            case 3 -> Items.COCOA_BEANS;
            case 4 -> Items.LAPIS_LAZULI;
            case 5 -> Items.PURPLE_DYE;
            case 6 -> Items.CYAN_DYE;
            case 7 -> Items.LIGHT_GRAY_DYE;
            case 8 -> Items.GRAY_DYE;
            case 9 -> Items.PINK_DYE;
            case 10 -> Items.LIME_DYE;
            case 11 -> Items.YELLOW_DYE;
            case 12 -> Items.LIGHT_BLUE_DYE;
            case 13 -> Items.MAGENTA_DYE;
            case 14 -> Items.ORANGE_DYE;
            case 15 -> Items.BONE_MEAL;
            default -> Items.INK_SAC;
        };
    }
}
