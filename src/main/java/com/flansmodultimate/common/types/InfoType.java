package com.flansmodultimate.common.types;

import com.flansmod.client.model.ModelBomb;
import com.flansmod.client.model.ModelBullet;
import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.util.ClassLoaderUtils;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.LogUtils;
import com.flansmodultimate.util.ModUtils;
import com.flansmodultimate.util.ResourceUtils;
import com.flansmodultimate.util.TypeReaderUtils;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;
import static com.flansmodultimate.util.TypeReaderUtils.readValues;

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
    protected String originalShortName = StringUtils.EMPTY;
    @Getter
    protected String icon = StringUtils.EMPTY;
    @Getter
    protected String description = StringUtils.EMPTY;
    protected String modelName = StringUtils.EMPTY;
    @Getter
    protected String modelClassName = StringUtils.EMPTY;
    protected String textureName = StringUtils.EMPTY;
    protected String overlayName = StringUtils.EMPTY;
    @Getter
    protected float modelScale = 1F;
    @Getter
    protected int colour = 0xFFFFFF;
    protected String[] recipeLine;
    protected char[][] recipeGrid = new char[3][3];
    protected int recipeOutput = 1;
    protected boolean shapeless;
    protected String smeltableFrom = StringUtils.EMPTY;
    /** If this is set to false, then this item cannot be dropped */
    protected boolean canDrop = true;
    /**
     * The probability that this item will appear in a dungeon chest.
     * Scaled so that each chest is likely to have a fixed number of Flan's Mod items.
     * Must be greater than or equal to 0, and should probably not exceed 100
     */
    protected int dungeonChance = 1;

    @Getter @OnlyIn(Dist.CLIENT)
    protected ResourceLocation texture;
    @Nullable @OnlyIn(Dist.CLIENT)
    protected ResourceLocation overlay;

    public String getShortName()
    {
        if (type.isItemType())
            return Objects.requireNonNull(ContentManager.getShortnameReferences().get(contentPack).get(originalShortName)).get();
        else
            return originalShortName;
    }

    @OnlyIn(Dist.CLIENT)
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

    public void read(TypeFile file)
    {
        fileName = file.getName();
        contentPack = file.getContentPack();
        type = file.getType();

        for (String line : file.getLines())
        {
            if (line.isBlank() || line.startsWith("//"))
                continue;

            String[] split = line.trim().split("\\s+");
            if (split.length == 0)
                continue;

            try
            {
                readLine(line, split, file);
            }
            catch (Exception e)
            {
                FlansMod.log.error("Reading line failed: {}", line);
                FlansMod.log.error("In file: {}", file);
                LogUtils.logWithoutStacktrace(e);
            }
        }

        postRead();
        if (FMLEnvironment.dist == Dist.CLIENT)
            postReadClient();
    }

    protected void readLine(String line, String[] split, TypeFile file)
    {
        name = readValues(split, "Name", name, file);
        originalShortName = ResourceUtils.sanitize(readValue(split, "ShortName", originalShortName, file));
        description = readValues(split, "Description", description, file);
        icon = ResourceUtils.sanitize(readValue(split, "Icon", icon, file));
        textureName = ResourceUtils.sanitize(readValue(split, "Texture", textureName, file));
        overlayName = ResourceUtils.sanitize(readValue(split, "Overlay", overlayName, file));
        modelName = readValue(split, "Model", modelName, file);
        modelScale = readValue(split, "ModelScale", modelScale, file);

        dungeonChance = readValue(split, "DungeonProbability", dungeonChance, file);
        dungeonChance = readValue(split, "DungeonLootChance", dungeonChance, file);

        recipeOutput = readValue(split, "RecipeOutput", recipeOutput, file);

        smeltableFrom = readValue(split, "SmeltableFrom", smeltableFrom, file);
        canDrop = readValue(split, "CanDrop", canDrop, file);

        // More complicated line reads
        if (split[0].equalsIgnoreCase("Colour") || split[0].equalsIgnoreCase("Color"))
        {
            colour = (Integer.parseInt(split[1]) << 16) + ((Integer.parseInt(split[2])) << 8) + ((Integer.parseInt(split[3])));
        }

        if (split[0].equalsIgnoreCase("Recipe"))
        {
            addToRecipeGrid(line, file);
            recipeLine = split;
            shapeless = false;
        }
        else if(split[0].equalsIgnoreCase("ShapelessRecipe"))
        {
            recipeLine = split;
            shapeless = true;
        }
    }

    private void addToRecipeGrid(String line, TypeFile file)
    {
        List<String> lines = file.getLines();
        int recipeLineIndex = lines.indexOf(line);
        int fromIndex = recipeLineIndex + 1;
        int toIndex = Math.min(fromIndex + 3, lines.size());
        String[] recipeRows = lines.subList(fromIndex, toIndex).toArray(new String[0]);

        for (int i = 0; i < recipeRows.length; i++)
        {
            String recipeRow = recipeRows[i];
            for (int j = 0; j < 3; j++)
            {
                recipeGrid[i][j] = j < recipeRow.length() ? recipeRow.charAt(j) : ' ';
            }
        }
    }

    protected static String readSound(String[] split, String key, String currentValue, TypeFile file)
    {
        String sound = readValue(split, key, currentValue, file);
        if (StringUtils.isNotBlank(sound))
        {
            sound = ResourceUtils.sanitize(sound);
            FlansMod.registerSound(sound, file);
        }
        return sound;
    }

    protected static void addEffects(List<String> effectValues, List<MobEffectInstance> effects, String line, TypeFile file, boolean ambient, boolean visible)
    {
        if (!effectValues.isEmpty())
        {
            try
            {
                int effectId = Integer.parseInt(effectValues.get(0));
                int duration = (effectValues.size() > 1) ? Integer.parseInt(effectValues.get(1)) : 250;
                int amplifier = (effectValues.size() > 2) ? Integer.parseInt(effectValues.get(2)) : 0;
                ambient = (effectValues.size() > 3) ? Boolean.parseBoolean(effectValues.get(3)) : ambient;
                visible = (effectValues.size() > 4) ? Boolean.parseBoolean(effectValues.get(4)) : visible;
                MobEffect effect = MobEffect.byId(effectId);
                if (effect != null)
                {
                    effects.add(new MobEffectInstance(effect,  duration, amplifier, ambient, visible));
                }
                else
                {
                    TypeReaderUtils.logError(String.format("Potion ID %s does not exist in '%s'", effectId, line), file);
                }
            }
            catch (NumberFormatException e)
            {
                TypeReaderUtils.logError(String.format("NumberFormatException in '%s'", line), file);
            }
        }
    }

    protected void postRead() {}

    @OnlyIn(Dist.CLIENT)
    protected void postReadClient()
    {
        modelClassName = findModelClass(modelName, contentPack);
        texture = loadTexture(textureName, this);
        overlay = loadOverlay(overlayName, this).orElse(null);
    }

    @OnlyIn(Dist.CLIENT)
    protected String getTexturePath(String textureName)
    {
        return "textures/" + type.getTextureFolderName() + "/" + textureName + ".png";
    }

    @Nullable
    protected IModelBase getDefaultModel()
    {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    protected static String findModelClass(String modelName, IContentProvider contentPack)
    {
        String modelClassName = StringUtils.EMPTY;
        if (!modelName.isBlank() && !modelName.equalsIgnoreCase("null") && !modelName.equalsIgnoreCase("none"))
        {
            String[] modelNameSplit = modelName.split("\\.");
            Path classFile;
            FileSystem fs = FileUtils.createFileSystem(contentPack);

            if (modelNameSplit.length > 1)
            {
                modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model." + modelNameSplit[0] + ".Model" + modelNameSplit[1];
                classFile = contentPack.getModelPath(modelClassName, fs);

                // Handle 1.12.2 package format
                if (!Files.exists(classFile))
                {
                    modelClassName = "com." + FlansMod.FLANSMOD_ID + "." + modelNameSplit[0] + ".client.model.Model" + modelNameSplit[1];
                    Path redirectFile = (fs != null) ? fs.getPath("redirect.info") : contentPack.getPath().resolve("redirect.info");

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
                    classFile = contentPack.getModelPath(modelClassName, fs);
                }
            }
            else
            {
                modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model.Model" + modelName;
                classFile = contentPack.getModelPath(modelClassName, fs);
            }

            if (!modelClassAlreadyRegisteredForContentPack(modelClassName, contentPack))
            {
                String actualClassName = modelClassName;
                if (hasModelConflictWithOtherContentPack(actualClassName, contentPack))
                {
                    IContentProvider otherContentPack = ContentManager.getRegisteredModels().get(modelClassName);
                    FileSystem otherFs = FileUtils.createFileSystem(otherContentPack);
                    Path otherClassFile = otherContentPack.getModelPath(modelClassName, otherFs);

                    if (FileUtils.filesHaveDifferentBytesContent(classFile, otherClassFile))
                    {
                        actualClassName = findNewValidClassName(modelClassName);
                        FlansMod.log.info("Duplicate model class name {} renamed at runtime to {} in [{}] to avoid a conflict with [{}].", modelClassName, actualClassName, contentPack.getName(), otherContentPack.getName());
                    }

                    FileUtils.closeFileSystem(otherFs, otherContentPack);
                }

                ContentManager.getRegisteredModels().putIfAbsent(actualClassName, contentPack);
                DynamicReference.storeOrUpdate(modelClassName, actualClassName, ContentManager.getModelReferences().get(contentPack));
            }

            FileUtils.closeFileSystem(fs, contentPack);
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
        ResourceLocation texture = TextureManager.INTENTIONAL_MISSING_TEXTURE;
        if (!textureName.isBlank())
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
        if (!overlayName.isBlank())
        {
            var refsMap = ContentManager.getGuiTextureReferences().get(type.getContentPack());

            refsMap.putIfAbsent(overlayName, new DynamicReference(overlayName));
            DynamicReference ref = refsMap.get(overlayName);

            if (ref != null)
                return Optional.of(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/" + ref.get() + ".png"));
        }
        return Optional.empty();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @OnlyIn(Dist.CLIENT)
    public static IModelBase loadModel(String modelClassName, InfoType type)
    {
        IModelBase model = null;
        if (!modelClassName.isBlank())
        {
            if (modelClassName.equalsIgnoreCase("com.flansmod.client.model.ModelBullet"))
                model = new ModelBullet();
            else if (modelClassName.equalsIgnoreCase("com.flansmod.client.model.ModelBomb"))
                model = new ModelBomb();
            else
            {
                DynamicReference actualClassName = ContentManager.getModelReferences().get(type.getContentPack()).get(modelClassName);
                if (actualClassName != null)
                {
                    try
                    {
                        model = (IModelBase) ClassLoaderUtils.loadAndModifyClass(type.getContentPack(), modelClassName, actualClassName.get()).getConstructor().newInstance();
                    }
                    catch (Exception | NoClassDefFoundError | ClassFormatError e)
                    {
                        FlansMod.log.error("Could not load model class {} for {}", modelClassName, type);
                        if (e instanceof IOException ioException && ioException.getCause() instanceof NoSuchFileException noSuchFileException)
                            FlansMod.log.error("File not found: {}", noSuchFileException.getFile());
                        else
                            LogUtils.logWithoutStacktrace(e);
                    }
                }
            }

        }

        if (model == null)
            model = type.getDefaultModel();

        if (model instanceof IFlanTypeModel<?> flanItemModel && flanItemModel.typeClass().isInstance(type))
            ((IFlanTypeModel<InfoType>) flanItemModel).setType(type);

        return model;
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
}
