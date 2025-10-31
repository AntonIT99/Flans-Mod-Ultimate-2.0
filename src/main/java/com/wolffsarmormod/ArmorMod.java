package com.wolffsarmormod;

import com.mojang.logging.LogUtils;
import com.wolffsarmormod.common.entity.Bullet;
import com.wolffsarmormod.common.item.IPaintableItem;
import com.wolffsarmormod.common.paintjob.Paintjob;
import com.wolffsarmormod.common.types.EnumType;
import com.wolffsarmormod.common.types.PaintableType;
import com.wolffsarmormod.config.ModClientConfigs;
import com.wolffsarmormod.config.ModCommonConfigs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Mod(ArmorMod.MOD_ID)
public class ArmorMod
{
    public static final String MOD_ID = "wolffsarmormod";
    public static final String FLANSMOD_ID = "flansmod";
    // Range for which sound packets are sent
    //TODO: test progressive sound volume
    public static final float SOUND_RANGE = 64F;
    public static final float SOUND_VOLUME = SOUND_RANGE / 16F;

    public static final String SOUND_BULLET = "bullet";
    public static final String SOUND_BULLETFLYBY = "bulletflyby";
    public static final String SOUND_UNLOCKNOTCH = "unlocknotch";
    public static final String SOUND_SKULLBOSSLAUGH = "skullboss_laugh";
    public static final String SOUND_SKULLBOSSSPAWN = "skullboss_spawn";

    public static final Logger log = LogUtils.getLogger();
    //TODO: Make forceRecompileAllPacks configurable (does not work with mod config)
    //TODO: forceRecompileAllPacks to true if mod version changed compared to last start up
    //TODO: unzip/rezip in separate temp file to make sure the process can be safely interrupted
    public static final boolean FORCE_RECOMPILE_ALL_PACKS = false;

    private static final Map<EnumType, List<RegistryObject<Item>>> items = new EnumMap<>(EnumType.class);
    private static final Map<String, RegistryObject<SoundEvent>> sounds = new HashMap<>();

    private static final DeferredRegister<Item> itemRegistry = DeferredRegister.create(ForgeRegistries.ITEMS, ArmorMod.FLANSMOD_ID);
    private static final DeferredRegister<CreativeModeTab> creativeModeTabRegistry = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArmorMod.MOD_ID);
    private static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ArmorMod.MOD_ID);
    private static final DeferredRegister<SoundEvent> soundEventRegistry = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ArmorMod.FLANSMOD_ID);

    // Register entities
    public static final RegistryObject<EntityType<Bullet>> bulletEntity = entityRegistry.register("bullet", () ->
        EntityType.Builder.<Bullet>of(Bullet::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(64)   // how far clients track it
            .updateInterval(1)              // ticks between velocity/pos updates; 1 for projectiles
            .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "bullet").toString()));

    public ArmorMod(FMLJavaModLoadingContext context)
    {
        Arrays.stream(EnumType.values()).forEach(type -> items.put(type, new ArrayList<>()));
        Mixins.addConfiguration(MOD_ID + ".mixins.json");

        IEventBus modEventBus = context.getModEventBus();

        // Configs
        context.registerConfig(ModConfig.Type.COMMON, ModCommonConfigs.CONFIG);
        context.registerConfig(ModConfig.Type.CLIENT, ModClientConfigs.CONFIG);

        // Registries
        itemRegistry.register(modEventBus);
        creativeModeTabRegistry.register(modEventBus);
        entityRegistry.register(modEventBus);
        soundEventRegistry.register(modEventBus);

        // Read content packs and register items & sounds
        ContentManager.INSTANCE.findContentInFlanFolder();
        ContentManager.INSTANCE.readContentPacks();

        registerCreativeModeTabs();
        registerSound(SOUND_BULLET);
        registerSound(SOUND_BULLETFLYBY);
        registerSound(SOUND_UNLOCKNOTCH);
        registerSound(SOUND_SKULLBOSSLAUGH);
        registerSound(SOUND_SKULLBOSSSPAWN);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void registerCreativeModeTabs()
    {
        //TODO: put bullets that are used for driveables in driveable tab (weaponType)
        registerCreativeTab("armors", items.get(EnumType.ARMOR));
        registerCreativeTab("guns", Stream.of(items.get(EnumType.GUN), items.get(EnumType.BULLET), items.get(EnumType.GRENADE)).flatMap(List::stream).toList());
    }

    private void registerCreativeTab(String name, List<RegistryObject<Item>> itemsForTab)
    {
        if (itemsForTab.isEmpty())
            return;

        creativeModeTabRegistry.register(name, () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab." + MOD_ID + "." + name))
            .icon(() -> new ItemStack(itemsForTab.get(ThreadLocalRandom.current().nextInt(0, items.size())).get()))
            .withSearchBar()
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> ro : itemsForTab)
                {
                    Item item = ro.get();

                    // If it’s paintable, emit stacks for each paintjob
                    if (item instanceof IPaintableItem<?> paintableItem)
                    {
                        PaintableType type = paintableItem.getPaintableType();
                        if (BooleanUtils.isTrue(ModCommonConfigs.addAllPaintjobsToCreative.get()))
                        {
                            for (Paintjob pj : type.getPaintjobs())
                            {
                                output.accept(paintableItem.makePaintjobStack(pj));
                            }
                        }
                        else
                        {
                            output.accept(paintableItem.makeDefaultPaintjobStack());
                        }
                    }
                    else
                    {
                        output.accept(item);
                    }
                }
            })
            .build());
    }

    public static void registerItem(String itemName, EnumType type, Supplier<? extends Item> initItem)
    {
        items.get(type).add(itemRegistry.register(itemName, initItem));
    }

    public static void registerSound(String soundName)
    {
        if (sounds.containsKey(soundName))
            return;

        RegistryObject<SoundEvent> soundEvent = soundEventRegistry.register(soundName, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ArmorMod.FLANSMOD_ID, soundName)));
        sounds.put(soundName, soundEvent);
    }

    public static List<RegistryObject<Item>> getItems()
    {
        return items.values().stream().flatMap(List::stream).toList();
    }

    public static Optional<RegistryObject<SoundEvent>> getSoundEvent(String soundName)
    {
        return Optional.ofNullable(sounds.get(soundName));
    }
}
