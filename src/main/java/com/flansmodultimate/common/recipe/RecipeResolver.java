package com.flansmodultimate.common.recipe;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.ModUtils;
import com.flansmodultimate.util.ResourceUtils;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
     * namespaced raw registered item id, reserved legacy equipment name, content-pack short name alias as {@code flansmod:<alias>},
     * registered {@code flansmod:<id>}, registered {@code flansmodapocalypse:<id>}, registered
     * {@code minecraft:<id>}, legacy item mapping, then vanilla registry path fallback.
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
    public static Optional<Identifier> resolveItemId(String token, @Nullable IContentProvider provider)
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
     * namespaced raw registered item id, reserved legacy equipment name, content-pack short name alias as {@code flansmod:<alias>},
     * registered {@code flansmod:<id>}, registered {@code flansmodapocalypse:<id>}, registered
     * {@code minecraft:<id>}, legacy item mapping, then vanilla registry path fallback.
     *
     * @param id       raw item id or legacy recipe token
     * @param damage   legacy metadata value parsed from tokens like {@code wool.11}
     * @param provider content pack context used for short name alias lookup
     * @return the resolved item id, or {@link Optional#empty()} when no known id can be found
     */
    public static Optional<Identifier> resolveItemId(String id, int damage, @Nullable IContentProvider provider)
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
    public static Identifier createFallbackItemId(String token)
    {
        String trimmedToken = token.trim();
        if (trimmedToken.contains(":"))
        {
            String[] split = trimmedToken.split(":", 2);
            return Identifier.fromNamespaceAndPath(ResourceUtils.sanitize(split[0]), ResourceUtils.sanitize(split[1]));
        }

        return Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, ResourceUtils.sanitize(trimmedToken));
    }

    private static <T> Optional<T> resolveRecipeItem(String rawId, int damage, @Nullable IContentProvider provider, RecipeItemResolution<T> resolution)
    {
        if (hasNamespace(rawId))
            return resolveNamespacedRecipeItem(rawId, damage, provider, resolution);

        String sanitizedId = ResourceUtils.sanitize(rawId);
        String aliasedId = ContentManager.getShortnameAliasInContentPack(sanitizedId, provider);

        // 1.7 resolved vanilla unlocalized names (item.pickaxeDiamond, item.bootsIron,
        // etc.) before Flan short names. Preserve that precedence for this reserved
        // vocabulary so a pending content registration cannot capture the token.
        Optional<T> result = Optional.empty();
        if (isLegacyEquipmentId(sanitizedId))
        {
            result = resolution.legacy(sanitizedId, damage);
            if (result.isPresent())
                return result;
        }

        result = resolution.flansmod(aliasedId);
        if (result.isPresent())
            return result;

        if (!aliasedId.equals(sanitizedId))
        {
            result = resolution.flansmod(sanitizedId);
            if (result.isPresent())
                return result;
        }

        result = resolution.registered(FlansMod.APOCALYPSE_ID, apocalypseRecipePath(sanitizedId));
        if (result.isPresent())
            return result;

        result = resolution.registered("minecraft", sanitizedId);
        if (result.isPresent())
            return result;

        result = resolution.legacy(sanitizedId, damage);
        if (result.isPresent())
            return result;

        return resolution.vanillaPath(sanitizedId);
    }

    private static boolean isLegacyEquipmentId(String id)
    {
        String[] prefixes = {"sword", "pickaxe", "hatchet", "axe", "shovel", "hoe",
            "helmet", "chestplate", "leggings", "boots"};
        String[] materials = {"wood", "stone", "iron", "steel", "gold", "diamond"};
        for (String prefix : prefixes)
        {
            if (!id.startsWith(prefix))
                continue;
            String material = id.substring(prefix.length());
            for (String candidate : materials)
            {
                if (candidate.equals(material))
                    return true;
            }
        }
        return false;
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

        if (namespace.equals(FlansMod.APOCALYPSE_ID))
            return resolution.registered(namespace, apocalypseRecipePath(path));

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

    private static String apocalypseRecipePath(String path)
    {
        return switch (path)
        {
            case "sulphuricacidbucket", "bucketsulphuricacid" -> "sulphuric_acid_bucket";
            default -> path;
        };
    }

    private static Optional<Identifier> resolveRawItemId(String id)
    {
        Identifier location = Identifier.tryParse(id);
        if (location != null && BuiltInRegistries.ITEM.containsKey(location))
            return Optional.of(location);

        return Optional.empty();
    }

    private static Optional<ItemStack> resolveRawRecipeElement(String id, int amount, int damage)
    {
        Identifier location = Identifier.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location))
            return Optional.empty();

        Item item = BuiltInRegistries.ITEM.getValue(location);
        if (item == null)
            return Optional.empty();

        ItemStack stack = new ItemStack(item, amount);
        if (damage > 0)
            stack.setDamageValue(damage);

        return Optional.of(stack);
    }

    private static Optional<Identifier> resolveFlansmodItemId(String id)
    {
        InfoType type = InfoType.getInfoType(id);
        if (type != null && type.getType().isHasItem())
            return Optional.of(Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, type.getShortName()));

        return resolveRegisteredItemId(FlansMod.FLANSMOD_ID, id);
    }

    private static Optional<Identifier> resolveRegisteredItemId(String namespace, String path)
    {
        Identifier location = Identifier.tryBuild(namespace, path);
        if (location != null && BuiltInRegistries.ITEM.containsKey(location))
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
        Identifier location = Identifier.tryBuild(namespace, path);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location))
            return Optional.empty();

        Item item = BuiltInRegistries.ITEM.getValue(location);
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

    private static final RecipeItemResolution<Identifier> ITEM_ID_RESOLUTION = new RecipeItemResolution<>()
    {
        @Override
        public Optional<Identifier> raw(String id)
        {
            return resolveRawItemId(id);
        }

        @Override
        public Optional<Identifier> flansmod(String id)
        {
            return resolveFlansmodItemId(id);
        }

        @Override
        public Optional<Identifier> registered(String namespace, String path)
        {
            return resolveRegisteredItemId(namespace, path);
        }

        @Override
        public Optional<Identifier> legacy(String id, int damage)
        {
            return getLegacyRecipeItemId(id, damage);
        }

        @Override
        public Optional<Identifier> vanillaPath(String id)
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

    private static Optional<Identifier> getLegacyRecipeItemId(String id, int damage)
    {
        String equipmentPath = legacyEquipmentPath(id);
        if (equipmentPath != null)
            return Optional.of(Identifier.fromNamespaceAndPath("minecraft", equipmentPath));
        return getLegacyRecipeItem(id, damage).flatMap(RecipeResolver::id);
    }

    @Nullable
    private static String legacyEquipmentPath(String id)
    {
        String tool = null;
        String material = null;
        String[] toolPrefixes = {"pickaxe", "hatchet", "shovel", "sword", "axe", "hoe"};
        for (String prefix : toolPrefixes)
        {
            if (id.startsWith(prefix))
            {
                tool = prefix.equals("hatchet") ? "axe" : prefix;
                material = id.substring(prefix.length());
                break;
            }
        }
        if (tool != null)
        {
            String modernMaterial = switch (material)
            {
                case "wood" -> "wooden";
                case "gold" -> "golden";
                case "steel" -> "iron";
                case "stone", "iron", "diamond" -> material;
                default -> null;
            };
            return modernMaterial == null ? null : modernMaterial + "_" + tool;
        }

        String[] armourSlots = {"helmet", "chestplate", "leggings", "boots"};
        for (String slot : armourSlots)
        {
            if (!id.startsWith(slot))
                continue;
            String armourMaterial = switch (id.substring(slot.length()))
            {
                case "steel" -> "iron";
                case "gold" -> "golden";
                case "iron", "diamond" -> id.substring(slot.length());
                default -> null;
            };
            return armourMaterial == null ? null : armourMaterial + "_" + slot;
        }
        return null;
    }

    private static Optional<ItemStack> getVanillaRegistryPathRecipeElement(String id, int amount)
    {
        return getVanillaRegistryPathRecipeItem(id).map(item -> new ItemStack(item, amount));
    }

    private static Optional<Identifier> getVanillaRegistryPathRecipeItemId(String id)
    {
        return getVanillaRegistryPathRecipeItem(id).flatMap(RecipeResolver::id);
    }

    private static Optional<ItemLike> getVanillaRegistryPathRecipeItem(String id)
    {
        String lookupPath = getLookupPath(id);
        for (Item item : BuiltInRegistries.ITEM)
        {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
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
            case "swordwood" -> Items.WOODEN_SWORD;
            case "swordstone" -> Items.STONE_SWORD;
            case "swordiron", "swordsteel" -> Items.IRON_SWORD;
            case "swordgold" -> Items.GOLDEN_SWORD;
            case "sworddiamond" -> Items.DIAMOND_SWORD;
            case "pickaxewood" -> Items.WOODEN_PICKAXE;
            case "pickaxestone" -> Items.STONE_PICKAXE;
            case "pickaxeiron", "pickaxesteel" -> Items.IRON_PICKAXE;
            case "pickaxegold" -> Items.GOLDEN_PICKAXE;
            case "pickaxediamond" -> Items.DIAMOND_PICKAXE;
            case "hatchetwood", "axewood" -> Items.WOODEN_AXE;
            case "hatchetstone", "axestone" -> Items.STONE_AXE;
            case "hatchetiron", "axeiron", "hatchetsteel", "axesteel" -> Items.IRON_AXE;
            case "hatchetgold", "axegold" -> Items.GOLDEN_AXE;
            case "hatchetdiamond", "axediamond" -> Items.DIAMOND_AXE;
            case "shovelwood" -> Items.WOODEN_SHOVEL;
            case "shovelstone" -> Items.STONE_SHOVEL;
            case "shoveliron", "shovelsteel" -> Items.IRON_SHOVEL;
            case "shovelgold" -> Items.GOLDEN_SHOVEL;
            case "shoveldiamond" -> Items.DIAMOND_SHOVEL;
            case "hoewood" -> Items.WOODEN_HOE;
            case "hoestone" -> Items.STONE_HOE;
            case "hoeiron", "hoesteel" -> Items.IRON_HOE;
            case "hoegold" -> Items.GOLDEN_HOE;
            case "hoediamond" -> Items.DIAMOND_HOE;
            case "helmetiron", "helmetsteel" -> Items.IRON_HELMET;
            case "chestplateiron", "chestplatesteel" -> Items.IRON_CHESTPLATE;
            case "leggingsiron", "leggingssteel" -> Items.IRON_LEGGINGS;
            case "bootsiron", "bootssteel" -> Items.IRON_BOOTS;
            case "helmetgold" -> Items.GOLDEN_HELMET;
            case "chestplategold" -> Items.GOLDEN_CHESTPLATE;
            case "leggingsgold" -> Items.GOLDEN_LEGGINGS;
            case "bootsgold" -> Items.GOLDEN_BOOTS;
            case "helmetdiamond" -> Items.DIAMOND_HELMET;
            case "chestplatediamond" -> Items.DIAMOND_CHESTPLATE;
            case "leggingsdiamond" -> Items.DIAMOND_LEGGINGS;
            case "bootsdiamond" -> Items.DIAMOND_BOOTS;
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
            case "reddust" -> Items.REDSTONE;
            case "netherquartz" -> Items.QUARTZ;
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
            case "pistonstickybase" -> Items.STICKY_PISTON;
            default -> null;
        };
        return Optional.ofNullable(item);
    }

    private static ItemStack stack(ItemLike item, int amount)
    {
        return new ItemStack(item, amount);
    }

    private static Optional<Identifier> id(ItemLike item)
    {
        Identifier location = BuiltInRegistries.ITEM.getKey(item.asItem());
        if (location == null)
            location = BuiltInRegistries.ITEM.getKey(item.asItem());
        return Optional.ofNullable(location);
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
