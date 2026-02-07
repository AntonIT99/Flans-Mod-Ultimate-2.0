package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.ModUtils;
import com.flansmodultimate.util.ResourceUtils;
import com.flansmodultimate.util.TypeReaderUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class InfoType
{
    @Getter
    private static final Map<String, InfoType> infoTypes = new HashMap<>();
    @Getter @Setter
    private static int totalDungeonChance = 0;

    @Getter
    protected String fileName;
    @Getter
    protected EnumType type;
    @Getter
    protected IContentProvider contentPack;
    @Getter
    protected String name = StringUtils.EMPTY;
    @Getter
    protected String originalShortName;
    @Getter
    protected String icon;
    @Getter
    protected String description = StringUtils.EMPTY;
    protected String modelName;
    @Getter
    protected String modelClassName;
    protected String textureName;
    protected String overlayName;
    @Getter
    protected float modelScale = 1F;
    @Getter
    protected int colour = 0xFFFFFF;
    protected final List<String> recipeLines = new ArrayList<>();
    protected char[][] recipeGrid = new char[3][3];
    protected int recipeOutput = 1;
    protected boolean shapeless;
    protected String smeltableFrom;
    /** If this is set to false, then this item cannot be dropped */
    protected boolean canDrop = true;
    /**
     * The probability that this item will appear in a dungeon chest.
     * Scaled so that each chest is likely to have a fixed number of Flan's Mod items.
     * Must be greater than or equal to 0, and should probably not exceed 100
     */
    protected int dungeonChance = 1;

    @Getter
    protected ResourceLocation texture;
    @Nullable
    protected ResourceLocation overlay;
    @Getter
    protected boolean additiveBlending;

    public String getShortName()
    {
        if (type.isItemType())
            return Objects.requireNonNull(ContentManager.getShortnameReferences().get(contentPack).get(originalShortName)).get();
        else
            return originalShortName;
    }

    public Optional<ResourceLocation> getOverlay()
    {
        return Optional.ofNullable(overlay);
    }

    @Override
    public String toString()
    {
        return String.format("%s item '%s' [%s] in [%s]", type, originalShortName, fileName, contentPack.getName());
    }

    public void onItemRegistration(String registeredItemId)
    {
        infoTypes.put(registeredItemId, this);
    }

    public void load(TypeFile file)
    {
        fileName = file.getName();
        contentPack = file.getContentPack();
        type = file.getType();
        read(file);
        if (FMLEnvironment.dist == Dist.CLIENT)
            readClient(file);
    }

    protected void read(TypeFile file)
    {
        name = readValues("Name", name, file);
        originalShortName = readResource("ShortName", originalShortName, file);
        description = readValues("Description", description, file);
        icon = readResource("Icon", icon, file);
        textureName = readResource("Texture", textureName, file);
        overlayName = readResource("Overlay", overlayName, file);
        modelName = readValue("Model", modelName, file);
        modelScale = readValue("ModelScale", modelScale, file);
        additiveBlending = readValue("AdditiveBlending", additiveBlending, file);

        dungeonChance = readValue("DungeonProbability", dungeonChance, file);
        dungeonChance = readValue("DungeonLootChance", dungeonChance, file);

        recipeOutput = readValue("RecipeOutput", recipeOutput, file);

        smeltableFrom = readResource("SmeltableFrom", smeltableFrom, file);
        canDrop = readValue("CanDrop", canDrop, file);

        readIntValues("Colour", file, 3).ifPresent(c -> colour = (c[0] << 16) + (c[1] << 8) + c[2]);
        readIntValues("Color", file, 3).ifPresent(c -> colour = (c[0] << 16) + (c[1] << 8) + c[2]);

        readLines("Recipe", file).ifPresent(lines ->
        {
            recipeLines.addAll(lines);
            addToRecipeGrid(recipeLines);
            shapeless = false;
        });
        readLines("ShapelessRecipe", file).ifPresent(lines ->
        {
            recipeLines.addAll(lines);
            shapeless = true;
        });
    }

    private void addToRecipeGrid(List<String> recipeRows)
    {
        for (int i = 0; i < recipeRows.size(); i++)
        {
            String recipeRow = recipeRows.get(i);
            for (int j = 0; j < 3; j++)
            {
                recipeGrid[i][j] = j < recipeRow.length() ? recipeRow.charAt(j) : ' ';
            }
        }
    }

    protected static String readResource(String key, String defaultValue, TypeFile file)
    {
        return ResourceUtils.sanitize(readValue(key, defaultValue, file));
    }

    protected static String readSound(String key, String defaultValue, TypeFile file)
    {
        String sound = readResource(key, defaultValue, file);
        if (StringUtils.isNotBlank(sound))
            FlansMod.registerSound(sound, file);
        return sound;
    }

    protected static void addEffects(String key, List<MobEffectInstance> effects, TypeFile file, boolean ambient, boolean visible)
    {
        readValuesInLines(key, file).ifPresent(lines -> lines.forEach(effectValues -> {
            if (effectValues.length > 0)
            {
                try
                {
                    int effectId = Integer.parseInt(effectValues[0]);
                    int duration = (effectValues.length > 1) ? Integer.parseInt(effectValues[1]) : 250;
                    int amplifier = (effectValues.length > 2) ? Integer.parseInt(effectValues[2]) : 0;
                    boolean isAmbient = (effectValues.length > 3) ? Boolean.parseBoolean(effectValues[3]) : ambient;
                    boolean isVisible = (effectValues.length > 4) ? Boolean.parseBoolean(effectValues[4]) : visible;
                    MobEffect effect = MobEffect.byId(effectId);
                    if (effect != null)
                    {
                        effects.add(new MobEffectInstance(effect,  duration, amplifier, isAmbient, isVisible));
                    }
                    else
                    {
                        TypeReaderUtils.logError(String.format("Potion ID %s does not exist in '%s %s'", effectId, key, String.join(StringUtils.SPACE, effectValues)), file);
                    }
                }
                catch (NumberFormatException e)
                {
                    TypeReaderUtils.logError(String.format("NumberFormatException in '%s %s'", key, String.join(StringUtils.SPACE, effectValues)), file);
                }
            }
        }));
    }

    @OnlyIn(Dist.CLIENT)
    protected void readClient(TypeFile file)
    {
        modelClassName = findModelClass(modelName, contentPack);
        texture = loadTexture(textureName, this);
        overlay = loadOverlay(overlayName, this).orElse(null);
    }

    protected String getTexturePath(String textureName)
    {
        return "textures/" + type.getTextureFolderName() + "/" + textureName + ".png";
    }

    @OnlyIn(Dist.CLIENT)
    protected static String findModelClass(String modelName, IContentProvider contentPack)
    {
        String modelClassName = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(modelName) && !modelName.equalsIgnoreCase("null") && !modelName.equalsIgnoreCase("none"))
        {
            String[] modelNameSplit = modelName.split("\\.");
            Path classFile;
            Optional<FileSystem> fs = Optional.ofNullable(FileUtils.createFileSystem(contentPack));

            if (modelNameSplit.length > 1)
            {
                modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model." + modelNameSplit[0] + ".Model" + modelNameSplit[1];
                classFile = contentPack.getModelPath(modelClassName, fs.orElse(null));

                // Handle 1.12.2 package format
                if (!Files.exists(classFile))
                {
                    modelClassName = "com." + FlansMod.FLANSMOD_ID + "." + modelNameSplit[0] + ".client.model.Model" + modelNameSplit[1];
                    Path redirectFile = fs.map(fileSystem -> fileSystem.getPath("redirect.info")).orElseGet(() -> contentPack.getPath().resolve("redirect.info"));

                    if (Files.exists(redirectFile))
                    {
                        try
                        {
                            List<String> lines = Files.readAllLines(redirectFile);
                            if (lines.size() > 1 && modelNameSplit[0].equals(lines.get(0)))
                            {
                                modelClassName = lines.get(1) + ".Model" + modelNameSplit[1];
                            }
                        }
                        catch (IOException e)
                        {
                            FlansMod.log.error("Could not open {}", redirectFile, e);
                        }
                    }
                    classFile = contentPack.getModelPath(modelClassName, fs.orElse(null));
                }
            }
            else
            {
                modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model.Model" + modelName;
                classFile = contentPack.getModelPath(modelClassName, fs.orElse(null));
            }

            if (!modelClassAlreadyRegisteredForContentPack(modelClassName, contentPack))
            {
                String actualClassName = modelClassName;
                if (hasModelConflictWithOtherContentPack(actualClassName, contentPack))
                {
                    IContentProvider otherContentPack = ContentManager.getRegisteredModels().get(modelClassName);
                    FileSystem otherFs = FileUtils.createFileSystem(otherContentPack);
                    Path otherClassFile = otherContentPack.getModelPath(modelClassName, otherFs);

                    if (FileUtils.filesHaveDifferentBytes(classFile, otherClassFile))
                    {
                        actualClassName = findNewValidClassName(modelClassName);
                        FlansMod.log.info("Duplicate model class name {} renamed at runtime to {} in [{}] to avoid a conflict with [{}].", modelClassName, actualClassName, contentPack.getName(), otherContentPack.getName());
                    }

                    FileUtils.closeFileSystem(otherFs, otherContentPack);
                }

                ContentManager.getRegisteredModels().putIfAbsent(actualClassName, contentPack);
                DynamicReference.storeOrUpdate(modelClassName, actualClassName, ContentManager.getModelReferences().get(contentPack));
            }

            FileUtils.closeFileSystem(fs.orElse(null), contentPack);
        }
        return modelClassName;
    }

    protected static boolean modelClassAlreadyRegisteredForContentPack(String modelClassName, IContentProvider contentPack) {
        if (ContentManager.getModelReferences().get(contentPack).containsKey(modelClassName))
        {
            String actualClassName = ContentManager.getModelReferences().get(contentPack).get(modelClassName).get();
            return ContentManager.getRegisteredModels().containsKey(actualClassName)
                    && ContentManager.getRegisteredModels().get(actualClassName).equals(contentPack);
        }
        return false;
    }

    protected static boolean hasModelConflictWithOtherContentPack(String modelClassName, IContentProvider contentPack)
    {
        return ContentManager.getRegisteredModels().containsKey(modelClassName) && !contentPack.equals(ContentManager.getRegisteredModels().get(modelClassName));
    }

    protected static String findNewValidClassName(String className)
    {
        String newClassName = className;
        for (int i = 2; ContentManager.getRegisteredModels().containsKey(newClassName); i++)
        {
            newClassName = className + "_" + i;
        }
        return newClassName;
    }

    @OnlyIn(Dist.CLIENT)
    public static ResourceLocation loadTexture(String textureName, InfoType type)
    {
        ResourceLocation texture = ResourceLocation.parse("");
        if (StringUtils.isNotBlank(textureName))
        {
            DynamicReference ref;
            Map<String, DynamicReference> refsMap;
            if (type instanceof ArmorType)
                refsMap = ContentManager.getArmorTextureReferences().get(type.getContentPack());
            else
                refsMap = ContentManager.getSkinsTextureReferences().get(type.getContentPack());

            refsMap.putIfAbsent(textureName, new DynamicReference(textureName));
            ref = refsMap.get(textureName);

            if (ref != null)
                texture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, type.getTexturePath(ref.get()));
        }
        return texture;
    }

    @OnlyIn(Dist.CLIENT)
    public static Optional<ResourceLocation> loadOverlay(String overlayName, InfoType type)
    {
        if (StringUtils.isNotBlank(overlayName) && !overlayName.equalsIgnoreCase("none"))
        {
            var refsMap = ContentManager.getGuiTextureReferences().get(type.getContentPack());

            refsMap.putIfAbsent(overlayName, new DynamicReference(overlayName));
            DynamicReference ref = refsMap.get(overlayName);

            if (ref != null)
                return Optional.of(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/" + ref.get() + ".png"));
        }
        return Optional.empty();
    }

    public static ItemStack getRecipeElement(String str, @Nullable IContentProvider provider)
    {
        String[] split = str.split("\\.");
        if (split.length == 0)
            return ItemStack.EMPTY;

        String id = split[0];
        int damage = split.length > 1 ? Short.parseShort(split[1]) : Short.MAX_VALUE;
        int amount = 1;

        return getRecipeElement(id, amount, damage, provider);
    }

    public static ItemStack getRecipeElement(String id, int amount, int damage, @Nullable IContentProvider provider)
    {
        // Do a handful of special cases, mostly legacy recipes
        switch (id.toLowerCase(Locale.ROOT))
        {
            case "dooriron":
                return new ItemStack(Items.IRON_DOOR, amount);
            case "clayitem":
                return new ItemStack(Items.CLAY_BALL, amount);
            case "iron_trapdoor":
                return new ItemStack(Blocks.IRON_TRAPDOOR, amount);
            case "trapdoor":
                return new ItemStack(Blocks.OAK_TRAPDOOR, amount);
            case "gunpowder":
                return new ItemStack(Items.GUNPOWDER, amount);
            case "ingotiron", "iron":
                return new ItemStack(Items.IRON_INGOT, amount);
            case "boat":
                return new ItemStack(Items.OAK_BOAT, amount);
            default:
                break;
        }

        // Try a "modid:itemid" style lookup, default to "minecraft:itemid" if no modid
        Optional<ItemStack> stack = ModUtils.getItemStack(id, amount, damage);
        if (stack.isPresent())
            return stack.get();


        // Then fallback to "flansmod:itemid" (get shortname alias if the item is in the same content pack)
        id = ContentManager.getShortnameAliasInContentPack(id, provider);
        stack = ModUtils.getItemStack(id, amount, damage);
        if (stack.isPresent())
            return stack.get();

        //TODO: special ingredients
        // OreIngredients, just pick an ingot
        /*if (SPECIAL_INGREDIENTS.containsKey(id))
        {
            Ingredient ing = SPECIAL_INGREDIENTS.get(id);
            if (ing.getMatchingStacks().length > 0)
                return ing.getMatchingStacks()[0];
        }*/

        FlansMod.log.warn("Could not find {} in recipe", id);
        return ItemStack.EMPTY;
    }

    @Nullable
    public static InfoType getInfoType(String id)
    {
        return infoTypes.get(id);
    }

    //TODO: implement addLoot() from 1.12.2 (and also override in PaintableType)
}
