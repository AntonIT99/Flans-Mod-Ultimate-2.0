package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.recipe.RecipeResolver;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    @Getter
    protected final List<String> recipeTokens = new ArrayList<>();
    @Getter
    protected final List<String> recipePattern = new ArrayList<>(3);
    @Getter
    protected char[][] recipeGrid = new char[3][3];
    @Getter
    protected int recipeOutput = 1;
    @Getter
    protected boolean shapeless;
    @Getter
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
        if (type.isHasItem())
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
        List<String> lines = file.getLines();
        for (int i = 0; i < lines.size(); i++)
        {
            String[] split = lines.get(i).split("\\s+");
            readLine(split, i, file);
        }
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

        readRecipeDefinitions(file);
    }

    protected void readLine(String[] split, int lineIndex, TypeFile file)
    {

    }

    private void readRecipeDefinitions(TypeFile file)
    {
        clearRecipeDefinition();

        List<String> lines = file.getLines();
        for (int i = 0; i < lines.size(); i++)
        {
            String line = lines.get(i);
            if (StringUtils.isBlank(line) || line.trim().startsWith("//"))
                continue;

            String[] split = line.trim().split("\\s+");
            if (split.length < 1)
                continue;

            if (split[0].equalsIgnoreCase("Recipe"))
            {
                clearRecipeDefinition();
                recipeTokens.addAll(Arrays.asList(split).subList(1, split.length));
                shapeless = false;

                for (int row = 0; row < 3; row++)
                {
                    String recipeRow = Objects.requireNonNullElse((i + row + 1 < lines.size()) ? lines.get(i + row + 1) : StringUtils.EMPTY, StringUtils.EMPTY);
                    if (recipeRow.length() > 3)
                        TypeReaderUtils.logError("Looks like a bad recipe in " + originalShortName + ". Double check whether '" + recipeRow + "' is supposed to be part of the recipe", file);

                    recipePattern.add(padRecipeRow(recipeRow));
                }
                addToRecipeGrid(recipePattern);
            }
            else if (split[0].equalsIgnoreCase("ShapelessRecipe"))
            {
                clearRecipeDefinition();
                recipeTokens.addAll(Arrays.asList(split).subList(1, split.length));
                shapeless = true;
            }
        }
    }

    private void clearRecipeDefinition()
    {
        recipeTokens.clear();
        recipePattern.clear();
        clearRecipeGrid();
        shapeless = false;
    }

    private void clearRecipeGrid()
    {
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
                recipeGrid[i][j] = ' ';
        }
    }

    private static String padRecipeRow(String recipeRow)
    {
        if (recipeRow.length() >= 3)
            return recipeRow.substring(0, 3);
        return StringUtils.rightPad(recipeRow, 3);
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

    public boolean hasCraftingRecipe()
    {
        return !recipeTokens.isEmpty();
    }

    public boolean hasSmeltingRecipe()
    {
        return StringUtils.isNotBlank(smeltableFrom);
    }

    protected static String readResource(String key, String defaultValue, TypeFile file)
    {
        return ResourceUtils.sanitize(readValue(key, defaultValue, file));
    }

    protected static String readFileNameResource(String key, String defaultValue, TypeFile file)
    {
        return ResourceUtils.sanitizeFileNameStem(readValue(key, defaultValue, file));
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
        return "textures/" + type.getTextureFolderName() + "/" + textureName + FileUtils.PNG_EXTENSION;
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
                String modelPackageName = String.join(".", Arrays.copyOf(modelNameSplit, modelNameSplit.length - 1));
                String modelSimpleName = modelNameSplit[modelNameSplit.length - 1];
                modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model." + modelPackageName + ".Model" + modelSimpleName;
                classFile = contentPack.getModelPath(modelClassName, fs.orElse(null));

                // Try 1.12.2 package format
                if (!Files.exists(classFile))
                {
                    if (modelNameSplit[0].equals("jamespostmodernweapons"))
                        modelNameSplit[0] = "modernweapons";

                    modelPackageName = String.join(".", Arrays.copyOf(modelNameSplit, modelNameSplit.length - 1));
                    modelClassName = "com." + FlansMod.FLANSMOD_ID + "." + modelPackageName + ".client.model.Model" + modelSimpleName;
                    Path redirectFile = fs.map(fileSystem -> fileSystem.getPath("redirect.info")).orElseGet(() -> contentPack.getPath().resolve("redirect.info"));

                    if (Files.exists(redirectFile))
                    {
                        try
                        {
                            List<String> lines = Files.readAllLines(redirectFile);
                            if (lines.size() > 1 && modelNameSplit[0].equals(lines.get(0)))
                            {
                                String redirectedPackageName = lines.get(1);
                                if (modelNameSplit.length > 2)
                                    redirectedPackageName += "." + String.join(".", Arrays.copyOfRange(modelNameSplit, 1, modelNameSplit.length - 1));
                                modelClassName = redirectedPackageName + ".Model" + modelSimpleName;
                            }
                        }
                        catch (IOException e)
                        {
                            FlansMod.log.error("Could not open {}", redirectFile, e);
                        }
                    }

                    classFile = contentPack.getModelPath(modelClassName, fs.orElse(null));

                    // Fallback to default
                    if (!Files.exists(classFile))
                        modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model." + modelPackageName + ".Model" + modelSimpleName;
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

                    if (FileUtils.isDifferentFileContent(classFile, otherClassFile, false))
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
                return Optional.of(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/" + ref.get() + FileUtils.PNG_EXTENSION));
        }
        return Optional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    protected ResourceLocation loadGuiTextureLocation(String textureName, ResourceLocation defaultTexture)
    {
        if (StringUtils.isBlank(textureName) || textureName.equalsIgnoreCase("none"))
            return defaultTexture;

        var refsMap = ContentManager.getGuiTextureReferences().get(contentPack);
        if (refsMap != null)
        {
            refsMap.putIfAbsent(textureName, new DynamicReference(textureName));
            DynamicReference ref = refsMap.get(textureName);
            if (ref != null)
                textureName = ref.get();
        }

        return ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/" + textureName + FileUtils.PNG_EXTENSION);
    }

    public static ItemStack getRecipeElement(String str, @Nullable IContentProvider provider)
    {
        return RecipeResolver.resolve(str, provider);
    }

    public static ItemStack getRecipeElement(String id, int amount, int damage, @Nullable IContentProvider provider)
    {
        return RecipeResolver.resolve(id, amount, damage, provider);
    }

    @Nullable
    public static InfoType getInfoType(String id)
    {
        return infoTypes.get(id);
    }

    //TODO: implement addLoot() from 1.12.2 (and also override in PaintableType)
}
