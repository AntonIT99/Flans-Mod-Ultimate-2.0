package com.wolffsarmormod.common.types;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.ContentManager;
import com.wolffsarmormod.IContentProvider;
import com.wolffsarmormod.util.DynamicReference;
import com.wolffsarmormod.util.FileUtils;
import com.wolffsarmormod.util.LogUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.wolffsarmormod.util.TypeReaderUtils.readValue;
import static com.wolffsarmormod.util.TypeReaderUtils.readValues;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class InfoType
{
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
    protected String description = StringUtils.EMPTY;
    protected String modelName = StringUtils.EMPTY;
    protected String modelClassName = StringUtils.EMPTY;
    protected String icon = StringUtils.EMPTY;
    protected String textureName = StringUtils.EMPTY;
    protected String overlayName = StringUtils.EMPTY;
    @Getter
    protected float modelScale = 1F;

    public void read(TypeFile file)
    {
        fileName = file.getName();
        contentPack = file.getContentPack();
        type = file.getType();

        for (String line : file.getLines())
        {
            if (line.isBlank() || line.startsWith("//"))
                continue;

            String[] split = line.split(StringUtils.SPACE);
            if (split.length == 0)
                continue;

            try
            {
                readLine(line, split, file);
            }
            catch (Exception e)
            {
                ArmorMod.log.error("Reading line failed: {}", line);
                ArmorMod.log.error("In file: {}", file);
                LogUtils.logWithoutStacktrace(e);
            }
        }

        postRead();
    }

    protected void readLine(String line, String[] split, TypeFile file)
    {
        //TODO: Recipes
        name = readValues(split, "Name", name, file);
        originalShortName = readValue(split, "ShortName", originalShortName, file).toLowerCase();
        description = readValues(split, "Description", description, file);
        icon = readValue(split, "Icon", icon, file).toLowerCase();
        textureName = readValue(split, "Texture", textureName, file).toLowerCase();
        overlayName = readValue(split, "Overlay", overlayName, file).toLowerCase();
        modelName = readValue(split, "Model", modelName, file);
        modelScale = readValue(split, "ModelScale", modelScale, file);
    }

    protected void postRead()
    {
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            findModelClass();
            if (!textureName.isBlank())
                ContentManager.getSkinsTextureReferences().get(contentPack).putIfAbsent(textureName, new DynamicReference(textureName));
            if (!overlayName.isBlank())
                ContentManager.getGuiTextureReferences().get(contentPack).putIfAbsent(overlayName, new DynamicReference(overlayName));
        }
    }

    protected String readSound(String[] split, String key, String currentValue, TypeFile file)
    {
        String sound = readValue(split, key, currentValue, file);
        if (StringUtils.isNotBlank(sound))
        {
            sound = sound.toLowerCase();
            ArmorMod.register(sound);
        }
        return sound;
    }

    @OnlyIn(Dist.CLIENT)
    protected void findModelClass()
    {
        if (!modelName.isBlank() && !modelName.equalsIgnoreCase("null") && !modelName.equalsIgnoreCase("none"))
        {
            String[] modelNameSplit = modelName.split("\\.");
            Path classFile;
            FileSystem fs = FileUtils.createFileSystem(contentPack);

            if (modelNameSplit.length > 1)
            {
                modelClassName = "com." + ArmorMod.FLANSMOD_ID + ".client.model." + modelNameSplit[0] + ".Model" + modelNameSplit[1];
                classFile = contentPack.getModelPath(modelClassName, fs);

                // Handle 1.12.2 package format
                if (!Files.exists(classFile))
                {
                    modelClassName = "com." + ArmorMod.FLANSMOD_ID + "." + modelNameSplit[0] + ".client.model.Model" + modelNameSplit[1];
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
                            ArmorMod.log.error("Could not open {}", redirectFile, e);
                        }
                    }
                    classFile = contentPack.getModelPath(modelClassName, fs);
                }
            }
            else
            {
                modelClassName = "com." + ArmorMod.FLANSMOD_ID + ".client.model.Model" + modelName;
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
                        ArmorMod.log.info("Duplicate model class name {} renamed at runtime to {} in [{}] to avoid a conflict with [{}].", modelClassName, actualClassName, contentPack.getName(), otherContentPack.getName());
                    }

                    FileUtils.closeFileSystem(otherFs, otherContentPack);
                }

                ContentManager.getRegisteredModels().putIfAbsent(actualClassName, contentPack);
                DynamicReference.storeOrUpdate(modelClassName, actualClassName, ContentManager.getModelReferences().get(contentPack));
            }

            FileUtils.closeFileSystem(fs, contentPack);
        }
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

    protected String findNewValidClassName(String className)
    {
        String newClassName = className;
        for (int i = 2; ContentManager.getRegisteredModels().containsKey(newClassName); i++)
        {
            newClassName = className + "_" + i;
        }
        return newClassName;
    }

    @OnlyIn(Dist.CLIENT)
    public String getIcon()
    {
        return icon;
    }

    @OnlyIn(Dist.CLIENT)
    public String getModelClassName()
    {
        return modelClassName;
    }

    public String getShortName()
    {
        return Objects.requireNonNull(ContentManager.getShortnameReferences().get(contentPack).get(originalShortName)).get();
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public DynamicReference getActualModelClassName()
    {
        if (!modelClassName.isBlank())
        {
            return ContentManager.getModelReferences().get(contentPack).get(modelClassName);
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getTexture()
    {
        if (!textureName.isBlank())
        {
            DynamicReference ref = ContentManager.getSkinsTextureReferences().get(contentPack).get(textureName);
            if (ref != null)
                return ResourceLocation.fromNamespaceAndPath(ArmorMod.FLANSMOD_ID, getTexturePath(ref.get()));
        }
        return TextureManager.INTENTIONAL_MISSING_TEXTURE;
    }

    @OnlyIn(Dist.CLIENT)
    protected String getTexturePath(String textureName)
    {
        return "textures/" + type.getTextureFolderName() + "/" + textureName + ".png";
    }

    @OnlyIn(Dist.CLIENT)
    public Optional<ResourceLocation> getOverlay()
    {
        if (!overlayName.isBlank())
        {
            DynamicReference ref = ContentManager.getGuiTextureReferences().get(contentPack).get(overlayName);
            if (ref != null)
                return Optional.of(ResourceLocation.fromNamespaceAndPath(ArmorMod.FLANSMOD_ID, "textures/gui/" + ref.get() + ".png"));
        }
        return Optional.empty();
    }

    @Override
    public String toString()
    {
        return String.format("%s item '%s' [%s] in [%s]", type, getShortName(), fileName, contentPack.getName());
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
        switch (id)
        {
            case "doorIron":
                return new ItemStack(Items.IRON_DOOR, amount);
            case "clayItem":
                return new ItemStack(Items.CLAY_BALL, amount);
            case "iron_trapdoor":
                return new ItemStack(Blocks.IRON_TRAPDOOR, amount);
            case "trapdoor":
                return new ItemStack(Blocks.OAK_TRAPDOOR, amount);
            case "gunpowder":
                return new ItemStack(Items.GUNPOWDER, amount);
            case "ingotIron", "iron":
                return new ItemStack(Items.IRON_INGOT, amount);
            case "boat":
                return new ItemStack(Items.OAK_BOAT, amount);
            default:
                break;
        }

        // Try a "modid:itemid" style lookup, default to "minecraft:itemid" if no modid
        Optional<ItemStack> stack = getItemStack(id, amount, damage);
        if (stack.isPresent())
            return stack.get();


        // Then fallback to "flansmod:itemid" (get shortname alias if the item is in the same content pack)
        id = ContentManager.getShortnameAliasInContentPack(id, provider);
        stack = getItemStack(id, amount, damage);
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

        ArmorMod.log.warn("Could not find {} in recipe", id);
        return ItemStack.EMPTY;
    }

    protected static Optional<ItemStack> getItemStack(@Nullable String id, int amount, int damage)
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

    protected static Optional<Item> getItemById(@Nullable String id)
    {
        if (id == null || id.isBlank())
            return Optional.empty();

        id = id.trim().toLowerCase();

        // If no namespace, assume minecraft
        if (!id.contains(":"))
            id = "minecraft:" + id;

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null)
            return Optional.empty();

        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(rl));
    }
}
