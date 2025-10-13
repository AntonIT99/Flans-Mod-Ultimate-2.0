package com.wolffsarmormod;

import com.mojang.logging.LogUtils;
import com.wolffsarmormod.common.types.EnumType;
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
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Mod(ArmorMod.MOD_ID)
public class ArmorMod
{
    public static final String MOD_ID = "wolffsarmormod";
    public static final String FLANSMOD_ID = "flansmod";
    public static final Logger log = LogUtils.getLogger();
    //TODO: Make forceRecompileAllPacks configurable (does not work with mod config)
    //TODO: forceRecompileAllPacks to true if mod version changed compared to last start up
    public static final boolean forceRecompileAllPacks = false;

    private static final Map<EnumType, List<RegistryObject<Item>>> items = new EnumMap<>(EnumType.class);
    private static final DeferredRegister<Item> itemRegistry = DeferredRegister.create(ForgeRegistries.ITEMS, ArmorMod.FLANSMOD_ID);
    private static final DeferredRegister<CreativeModeTab> creativeModeTabRegistry = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArmorMod.MOD_ID);

    public ArmorMod(FMLJavaModLoadingContext context)
    {
        Mixins.addConfiguration(MOD_ID + ".mixins.json");

        IEventBus eventBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.COMMON, ModCommonConfigs.CONFIG);
        context.registerConfig(ModConfig.Type.CLIENT, ModClientConfigs.CONFIG);

        Arrays.stream(EnumType.values()).forEach(type -> items.put(type, new ArrayList<>()));
        itemRegistry.register(eventBus);
        creativeModeTabRegistry.register(eventBus);

        ContentManager.INSTANCE.findContentInFlanFolder();
        ContentManager.INSTANCE.readContentPacks();
        registerCreativeModeTabs();

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void registerCreativeModeTabs()
    {
        registerCreativeTab("armors", items.get(EnumType.ARMOR));
        registerCreativeTab("guns", Stream.of(items.get(EnumType.GUN), items.get(EnumType.BULLET), items.get(EnumType.GRENADE)).flatMap(List::stream).toList());
    }

    private void registerCreativeTab(String name, List<RegistryObject<Item>> itemsForTab) {
        if (itemsForTab.isEmpty()) return;

        creativeModeTabRegistry.register(name, () -> CreativeModeTab.builder()
                .title(Component.translatable("creativetab." + MOD_ID + "." + name))
                .icon(() -> new ItemStack(itemsForTab.get(ThreadLocalRandom.current().nextInt(0, items.size())).get()))
                .displayItems((parameters, output) -> itemsForTab.forEach(item -> output.accept(item.get())))
                .withSearchBar()
                .build());
    }

    public static void registerItem(String itemName, EnumType type, Supplier<? extends Item> initItem)
    {
        items.get(type).add(itemRegistry.register(itemName, initItem));
    }
}
