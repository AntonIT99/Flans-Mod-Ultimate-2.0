package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.api.IInfoType;
import com.flansmodultimate.common.guns.AmmoOverrides;
import com.flansmodultimate.common.recipe.RecipeResolver;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.ModUtils;
import com.flansmodultimate.util.ResourceUtils;
import com.flansmodultimate.util.SoundLengthIndex;
import com.flansmodultimate.util.TypeReaderUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import java.util.OptionalInt;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class InfoType implements IInfoType
{
    private static final String LOOT_POOL_NAME = "FlansMod";

    /** A timer telling the mod when to play a sound again, paired with the sound it plays. */
    private record SoundTimer(String parameterName, Supplier<String> sound, IntSupplier length, IntConsumer applyLength) {}

    /** Populated while reading a type and cleared once the measured sound lengths have been applied. */
    private final List<SoundTimer> soundTimers = new ArrayList<>();

    @Getter
    private static final Map<String, InfoType> infoTypes = new HashMap<>();
    @Getter @Setter
    private static int totalDungeonChance = 0;
    private static final ThreadLocal<LootBuildContext> activeLootBuildContext = new ThreadLocal<>();
    private static boolean lootReflectionFailureLogged;

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
    /**
     * Unique name of an item-less type, assigned by its {@link ItemlessTypeRegistry} and differing
     * from {@link #originalShortName} only when two content packs picked the same shortname.
     * Types that have an item take their unique name from the item registry instead.
     */
    @Nullable
    protected String uniqueShortName;
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
    @Getter
    protected boolean canDrop = true;
    /**
     * The probability that this item will appear in a dungeon chest.
     * Scaled so that each chest is likely to have a fixed number of Flan's Mod items.
     * Must be greater than or equal to 0, and should probably not exceed 100
     */
    @Getter
    protected int dungeonChance = 1;

    @Getter
    protected ResourceLocation texture;
    @Nullable
    protected ResourceLocation overlay;
    @Getter
    protected RenderOptions renderOptions;

    public record RenderOptions(boolean translucentRendering, boolean additiveBlending, boolean disableCulling) {}

    /**
     * Reads the optional per-ammunition override keys and reports any malformed line
     * as an ordinary content warning. Shared by every weapon type that can fire.
     */
    protected AmmoOverrides readAmmoOverrides(TypeFile file)
    {
        AmmoOverrides.Result result = AmmoOverrides.read(file);
        for (String warning : result.warnings())
            TypeReaderUtils.logError(warning, file);
        return result.overrides();
    }

    /**
     * The name this type is uniquely reachable under across every loaded content pack. It equals
     * {@link #getOriginalShortName()} unless another pack claimed that shortname first, in which
     * case this carries the {@code _2} alias. Always prefer it over the original shortname when
     * storing an identifier in a save, a packet or a command.
     */
    public String getShortName()
    {
        if (type != null && type.isHasItem())
            return Objects.requireNonNull(ContentManager.getShortnameReferences().get(contentPack).get(originalShortName)).get();
        return uniqueShortName != null ? uniqueShortName : originalShortName;
    }

    @Override
    @Nullable
    public Item getItem()
    {
        if (type == null || !type.isHasItem())
            return null;
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, getShortName()));
        return item == null || item == Items.AIR ? null : item;
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
        if (PlatformEnvironment.isClient())
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
        boolean translucentRendering = readValue("TranslucentRendering", false, file);
        boolean additiveBlending = readValue("AdditiveBlending", false, file);
        boolean disableCulling = readValue("DisableCulling", false, file);
        renderOptions = new RenderOptions(translucentRendering, additiveBlending, disableCulling);

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
                    String recipeRow = getRecipeRow((i + row + 1 < lines.size()) ? lines.get(i + row + 1) : StringUtils.EMPTY);
                    if (hasRecipeContentAfterGrid(recipeRow))
                        TypeReaderUtils.logError("Looks like a bad recipe in " + originalShortName + ". Double check whether '" + recipeRow + "' is supposed to be part of the recipe", file);

                    recipePattern.add(padRecipeRow(recipeRow));
                }
                addToRecipeGrid(recipePattern);
                i += 3;
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
        recipeRow = recipeRow.replace('.', ' ');
        if (recipeRow.length() >= 3)
            return recipeRow.substring(0, 3);
        return StringUtils.rightPad(recipeRow, 3);
    }

    /**
     * Legacy content packs use both bare recipe rows and rows prefixed with
     * {@code Recipe}. The latter must not be interpreted as a new declaration.
     */
    private static String getRecipeRow(String recipeRow)
    {
        String row = Objects.requireNonNullElse(recipeRow, StringUtils.EMPTY);
        if (row.regionMatches(true, 0, "Recipe", 0, "Recipe".length())
            && row.length() > "Recipe".length()
            && Character.isWhitespace(row.charAt("Recipe".length())))
            return row.substring("Recipe".length() + 1);
        return row;
    }

    private static boolean hasRecipeContentAfterGrid(String recipeRow)
    {
        return recipeRow.length() > 3 && StringUtils.isNotBlank(recipeRow.substring(3));
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

    /**
     * Legacy packs commonly leave optional scalar fields present but empty.
     * For those explicitly optional call sites, an empty field is equivalent
     * to omitting it and must not be reported as corrupt data.
     */
    protected static String readOptionalValue(String key, String defaultValue, TypeFile file)
    {
        return hasValueForConfigField(key, file) ? readValue(key, defaultValue, file) : defaultValue;
    }

    protected static int readOptionalValue(String key, int defaultValue, TypeFile file)
    {
        return hasValueForConfigField(key, file) ? readValue(key, defaultValue, file) : defaultValue;
    }

    protected static float readOptionalValue(String key, float defaultValue, TypeFile file)
    {
        return hasValueForConfigField(key, file) ? readValue(key, defaultValue, file) : defaultValue;
    }

    protected static String readSound(String key, String defaultValue, TypeFile file)
    {
        if (!file.hasConfigLine(key))
            return defaultValue;

        // A number of established content packs use a key without a value, or
        // the literal "None", to explicitly disable a sound. Treat both forms
        // as valid legacy syntax instead of emitting a misleading parse error
        // or attempting to register a resource named "none".
        if (!hasValueForConfigField(key, file))
            return StringUtils.EMPTY;

        String configuredSound = readValue(key, defaultValue, file);
        if (StringUtils.equalsIgnoreCase(configuredSound, "none"))
            return StringUtils.EMPTY;

        String sound = ResourceUtils.sanitize(configuredSound);
        if (StringUtils.isNotBlank(sound))
            FlansMod.registerSound(sound, file);
        return sound;
    }

    /** Reads legacy sound timer fields, where blank and {@code None} mean disabled. */
    protected static int readSoundLength(String key, int defaultValue, TypeFile file)
    {
        if (!file.hasConfigLine(key))
            return defaultValue;
        if (!hasValueForConfigField(key, file))
            return 0;

        String configuredLength = readValue(key, (String)null, file);
        if (StringUtils.equalsIgnoreCase(configuredLength, "none"))
            return 0;
        try
        {
            return Integer.parseInt(configuredLength);
        }
        catch (NumberFormatException exception)
        {
            TypeReaderUtils.logError("Incorrect format for '" + key + "': expected an integer but found '"
                + configuredLength + "'", file);
            return defaultValue;
        }
    }

    /**
     * Declares that a sound timer plays the given sound, so it can be replaced with the measured
     * length of that sound file once every content pack has been read.
     *
     * @param parameterName the config parameter the timer is read from, for logging
     * @param sound         the sound the timer plays, read at resolution time
     * @param length        reads the configured timer value
     * @param applyLength   replaces the timer value
     */
    protected void registerSoundTimer(String parameterName, Supplier<String> sound, IntSupplier length, IntConsumer applyLength)
    {
        soundTimers.add(new SoundTimer(parameterName, sound, length, applyLength));
    }

    /**
     * Replaces every registered sound timer with the real length of the sound it plays.
     * <p>
     * Only timers that a content pack actually configured are touched. A timer left at zero means the
     * sound is not repeated at all, either because the pack disabled it with {@code None} or because
     * the parameter defaults to zero, and filling one in would start repeating a sound that is meant
     * to play once.
     *
     * @return how many timers were replaced
     */
    public int resolveSoundLengths()
    {
        int resolved = 0;
        for (SoundTimer timer : soundTimers)
        {
            String sound = timer.sound().get();
            int configuredLength = timer.length().getAsInt();
            if (StringUtils.isBlank(sound) || configuredLength <= 0)
                continue;

            OptionalInt measuredLength = SoundLengthIndex.getSoundLength(sound);
            if (measuredLength.isEmpty() || measuredLength.getAsInt() == configuredLength)
                continue;

            timer.applyLength().accept(measuredLength.getAsInt());
            resolved++;
            FlansMod.log.debug("{}: {} of sound '{}' changed from {} to the measured {} tick(s)",
                originalShortName, timer.parameterName(), sound, configuredLength, measuredLength.getAsInt());
        }

        soundTimers.clear();
        return resolved;
    }

    protected static void addEffects(String key, List<MobEffectInstance> effects, TypeFile file, boolean ambient, boolean visible)
    {
        addEffects(key, effects, file, ambient, visible, 250, 0);
    }

    protected static void addEffects(String key, List<MobEffectInstance> effects, TypeFile file, boolean ambient, boolean visible,
                                     int defaultDuration, int defaultAmplifier)
    {
        readValuesInLines(key, file).ifPresent(lines -> lines.forEach(effectValues -> {
            if (effectValues.length > 0)
            {
                try
                {
                    int effectId = Integer.parseInt(effectValues[0]);
                    int duration = (effectValues.length > 1) ? Integer.parseInt(effectValues[1]) : defaultDuration;
                    int amplifier = (effectValues.length > 2) ? Integer.parseInt(effectValues[2]) : defaultAmplifier;
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

    /**
     * Resolves the model class name a {@code Model} entry refers to inside its own content pack. Content
     * packs are free to ship equally named model classes, every pack loads its own class files.
     */
    @OnlyIn(Dist.CLIENT)
    protected static String findModelClass(String modelName, IContentProvider contentPack)
    {
        String modelClassName = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(modelName) && !modelName.equalsIgnoreCase("null") && !modelName.equalsIgnoreCase("none"))
        {
            String[] modelNameSplit = modelName.split("\\.");
            Optional<FileSystem> fs = Optional.ofNullable(FileUtils.createFileSystem(contentPack));

            if (modelNameSplit.length > 1)
            {
                String modelPackageName = String.join(".", Arrays.copyOf(modelNameSplit, modelNameSplit.length - 1));
                String modelSimpleName = modelNameSplit[modelNameSplit.length - 1];
                modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model." + modelPackageName + ".Model" + modelSimpleName;

                // Try 1.12.2 package format
                if (!Files.exists(contentPack.getModelPath(modelClassName, fs.orElse(null))))
                {
                    if (modelNameSplit[0].equals("jamespostmodernweapons"))
                        modelNameSplit[0] = "modernweapons";

                    modelPackageName = String.join(".", Arrays.copyOf(modelNameSplit, modelNameSplit.length - 1));
                    modelClassName = "com." + FlansMod.FLANSMOD_ID + "." + modelPackageName + ".client.model.Model" + modelSimpleName;
                    Path redirectFile = contentPack.getContentRoot(fs.orElse(null)).resolve("redirect.info");

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

                    // Fallback to default
                    if (!Files.exists(contentPack.getModelPath(modelClassName, fs.orElse(null))))
                        modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model." + modelPackageName + ".Model" + modelSimpleName;
                }
            }
            else
            {
                modelClassName = "com." + FlansMod.FLANSMOD_ID + ".client.model.Model" + modelName;
            }

            FileUtils.closeFileSystem(fs.orElse(null), contentPack);
        }
        return modelClassName;
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

    public static void beginLootTableLoad(LootTableLoadEvent event)
    {
        activeLootBuildContext.set(new LootBuildContext(event));
    }

    public static void finishLootTableLoad(LootTableLoadEvent event)
    {
        LootBuildContext context = activeLootBuildContext.get();
        activeLootBuildContext.remove();
        if (context == null || context.event != event || context.entryCount == 0)
            return;

        LootPool pool = event.getTable().getPool(LOOT_POOL_NAME);
        if (pool == null)
        {
            event.getTable().addPool(context.builder.build());
            return;
        }

        Optional<LootPool> appendedPool = Optional.of(pool);
        for (LootPoolEntryContainer entry : context.entries)
        {
            LootPool poolToAppend = appendedPool.orElse(null);
            if (poolToAppend == null)
                return;
            appendedPool = createAppendedPool(poolToAppend, entry);
        }

        if (appendedPool.isPresent())
        {
            event.getTable().removePool(LOOT_POOL_NAME);
            event.getTable().addPool(appendedPool.get());
        }
    }

    public void addLoot(LootTableLoadEvent event)
    {
        if (dungeonChance <= 0 || !type.isHasItem())
            return;

        ModUtils.getItem(this)
            .map(item -> createDungeonLootEntry(item, FlansMod.DUNGEON_LOOT_CHANCE * dungeonChance))
            .ifPresent(entry -> addLootEntry(event, entry));
    }

    protected LootPoolEntryContainer createDungeonLootEntry(Item item, int weight)
    {
        return LootItem.lootTableItem(item)
            .setWeight(weight)
            .setQuality(1)
            .build();
    }

    protected void addLootEntry(LootTableLoadEvent event, LootPoolEntryContainer entry)
    {
        LootBuildContext context = activeLootBuildContext.get();
        if (context != null && context.event == event)
        {
            context.add(entry);
            return;
        }

        LootPool pool = event.getTable().getPool(LOOT_POOL_NAME);
        if (pool == null)
        {
            event.getTable().addPool(LootPool.lootPool()
                .name(LOOT_POOL_NAME)
                .setRolls(ConstantValue.exactly(1F))
                .setBonusRolls(ConstantValue.exactly(1F))
                .add(new ExistingLootEntryBuilder(entry))
                .build());
            return;
        }

        Optional<LootPool> appendedPool = createAppendedPool(pool, entry);
        if (appendedPool.isPresent())
        {
            event.getTable().removePool(LOOT_POOL_NAME);
            event.getTable().addPool(appendedPool.get());
        }
    }

    private static Optional<LootPool> createAppendedPool(LootPool pool, LootPoolEntryContainer entry)
    {
        try
        {
            Field entriesField = LootPool.class.getDeclaredField("entries");
            Field conditionsField = LootPool.class.getDeclaredField("conditions");
            Field functionsField = LootPool.class.getDeclaredField("functions");
            entriesField.setAccessible(true);
            conditionsField.setAccessible(true);
            functionsField.setAccessible(true);

            LootPoolEntryContainer[] oldEntries = (LootPoolEntryContainer[]) entriesField.get(pool);
            LootPoolEntryContainer[] newEntries = Arrays.copyOf(oldEntries, oldEntries.length + 1);
            newEntries[oldEntries.length] = entry;

            Constructor<LootPool> constructor = LootPool.class.getDeclaredConstructor(
                LootPoolEntryContainer[].class,
                LootItemCondition[].class,
                LootItemFunction[].class,
                NumberProvider.class,
                NumberProvider.class,
                String.class
            );
            constructor.setAccessible(true);

            return Optional.of(constructor.newInstance(
                newEntries,
                (LootItemCondition[]) conditionsField.get(pool),
                (LootItemFunction[]) functionsField.get(pool),
                pool.getRolls(),
                pool.getBonusRolls(),
                LOOT_POOL_NAME
            ));
        }
        catch (Exception ex)
        {
            if (!lootReflectionFailureLogged)
            {
                FlansMod.log.error("Could not append Flan's Mod dungeon loot entries", ex);
                lootReflectionFailureLogged = true;
            }
            return Optional.empty();
        }
    }

    private static class LootBuildContext
    {
        private final LootTableLoadEvent event;
        private final LootPool.Builder builder = LootPool.lootPool()
            .name(LOOT_POOL_NAME)
            .setRolls(ConstantValue.exactly(1F))
            .setBonusRolls(ConstantValue.exactly(1F));
        private final List<LootPoolEntryContainer> entries = new ArrayList<>();
        private int entryCount;

        private LootBuildContext(LootTableLoadEvent event)
        {
            this.event = event;
        }

        private void add(LootPoolEntryContainer entry)
        {
            entries.add(entry);
            builder.add(new ExistingLootEntryBuilder(entry));
            entryCount++;
        }
    }

    private static class ExistingLootEntryBuilder extends LootPoolEntryContainer.Builder<ExistingLootEntryBuilder>
    {
        private final LootPoolEntryContainer entry;

        private ExistingLootEntryBuilder(LootPoolEntryContainer entry)
        {
            this.entry = entry;
        }

        @Override
        protected ExistingLootEntryBuilder getThis()
        {
            return this;
        }

        @Override
        public LootPoolEntryContainer build()
        {
            return entry;
        }
    }

    @Nullable
    public static InfoType getInfoType(String id)
    {
        return infoTypes.get(id);
    }

    @Nullable
    public static InfoType getInfoType(String id, @Nullable IContentProvider provider)
    {
        if (StringUtils.isBlank(id))
            return null;

        String rawId = id.trim();
        if (rawId.contains(":"))
        {
            String[] split = rawId.split(":", 2);
            if (!ResourceUtils.sanitize(split[0]).equals(FlansMod.FLANSMOD_ID))
                return null;
            rawId = split[1];
        }

        String sanitizedId = ResourceUtils.sanitize(rawId);
        String aliasedId = ContentManager.getShortnameAliasInContentPack(sanitizedId, provider);
        InfoType type = getInfoType(aliasedId);
        if (type == null && !aliasedId.equals(sanitizedId))
            type = getInfoType(sanitizedId);
        return type;
    }

}
