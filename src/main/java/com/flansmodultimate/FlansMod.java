package com.flansmodultimate;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.recipe.GunpowderRecipeCondition;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.config.CategoryManager;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.content.ContentManager;
import com.flansmodultimate.platform.network.NetworkPlatform;
import com.flansmodultimate.platform.registry.RegistryEntry;
import com.flansmodultimate.platform.world.ChunkTicketPlatform;
import com.flansmodultimate.util.ModLogFile;
import lombok.Getter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Mod(FlansMod.MOD_ID)
public class FlansMod
{
    public static final String MOD_ID = "flansmodultimate";
    public static final String FLANSMOD_ID = "flansmod";
    public static final String APOCALYPSE_ID = "flansmodapocalypse";
    public static final String PACKS_MANAGER_ID = "flansmodultimate_packs_manager";

    public static final TeamsManager teamsManager = new TeamsManager();
    public static final int DUNGEON_LOOT_CHANCE = 500;

    // Resource Locations
    public static final ResourceLocation PAINTJOB = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "paintjob");
    public static final ResourceLocation THROWING = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "throwing");

    private static final Map<EnumType, List<RegistryEntry<Item>>> items = new EnumMap<>(EnumType.class);
    @Getter
    private static final Map<EnumType, Map<String, RegistryEntry<Block>>> blocks = new EnumMap<>(EnumType.class);
    private static final Map<ResourceLocation, RegistryEntry<SoundEvent>> sounds = new HashMap<>();
    @Getter
    private static final Map<ResourceLocation, TypeFile> soundsOrigins = new HashMap<>();

    public FlansMod(IEventBus modEventBus, ModContainer modContainer)
    {
        GunpowderRecipeCondition.CODECS.register(modEventBus);
        modEventBus.addListener(NetworkPlatform::register);
        modEventBus.addListener(ChunkTicketPlatform::register);
        ModLogFile.initialize(MOD_ID);
        Arrays.stream(EnumType.values()).filter(EnumType::isHasItem).forEach(type -> items.put(type, new ArrayList<>()));
        Arrays.stream(EnumType.values()).filter(EnumType::isHasBlock).forEach(type -> blocks.put(type, new HashMap<>()));
        // Init Configs
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.configSpec);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModApocalypseConfig.configSpec, ModApocalypseConfig.CONFIG_FILE_NAME);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.configSpec);

        ApocalypseContent.register(modEventBus);

        // Init Registries
        FlansModRegistries.register(modEventBus);

        PacksManagerExtraction.waitForPacksManagerExtractionIfPresent();

        // Register Everything
        CategoryManager.loadAll();
        ContentManager.reconcileContentPackLocations();
        ContentManager.findContentInFlanFolder();
        ContentManager.readContentPacks();
        FlansModSounds.registerSounds();
        FlansModCreativeTabs.registerCreativeModeTabs();
    }

    static Block[] getRegisteredBlocks(EnumType type)
    {
        Map<String, RegistryEntry<Block>> registeredBlocks = blocks.get(type);
        if (registeredBlocks == null)
            return new Block[0];
        return registeredBlocks.values().stream().map(RegistryEntry::get).toArray(Block[]::new);
    }

    public static void registerItem(String itemName, EnumType type, Supplier<? extends Item> initItem)
    {
        items.get(type).add(RegistryEntry.of(FlansModRegistries.itemRegistry.register(itemName, initItem)));
    }

    public static void registerBlock(String blockName, EnumType type, Supplier<? extends Block> initItem)
    {
        blocks.get(type).put(blockName, RegistryEntry.of(FlansModRegistries.blockRegistry.register(blockName, initItem)));
    }

    public static void registerSound(String soundName, @Nullable TypeFile typeFile)
    {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName);
        if (sounds.containsKey(rl))
            return;

        RegistryEntry<SoundEvent> soundEvent = RegistryEntry.of(FlansModRegistries.soundEventRegistry.register(soundName, () -> SoundEvent.createVariableRangeEvent(rl)));
        sounds.put(rl, soundEvent);
        if (typeFile != null)
            soundsOrigins.put(rl, typeFile);
    }

    @Unmodifiable
    public static List<RegistryEntry<Item>> getItems()
    {
        return items.values().stream().flatMap(List::stream).toList();
    }

    public static List<RegistryEntry<Item>> getItems(EnumType type)
    {
        return items.get(type);
    }

    @Unmodifiable
    public static List<RegistryEntry<Item>> getItems(Set<EnumType> types)
    {
        return types.stream().map(items::get).flatMap(List::stream).toList();
    }

    public static Optional<RegistryEntry<SoundEvent>> getSoundEvent(String soundName)
    {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName);
        return Optional.ofNullable(sounds.get(rl));
    }
}
