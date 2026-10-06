package com.flansmodultimate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FlansModRegistries
{
    static final DeferredRegister<Block> blockRegistry = DeferredRegister.create(Registries.BLOCK, FlansMod.FLANSMOD_ID);
    static final DeferredRegister<Item> itemRegistry = DeferredRegister.create(Registries.ITEM, FlansMod.FLANSMOD_ID);
    static final DeferredRegister<MenuType<?>> menuRegistry = DeferredRegister.create(Registries.MENU, FlansMod.MOD_ID);
    static final DeferredRegister<ParticleType<?>> particleRegistry = DeferredRegister.create(Registries.PARTICLE_TYPE, FlansMod.FLANSMOD_ID);
    static final DeferredRegister<SoundEvent> soundEventRegistry = DeferredRegister.create(Registries.SOUND_EVENT, FlansMod.FLANSMOD_ID);
    static final DeferredRegister<CreativeModeTab> creativeModeTabRegistry = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FlansMod.MOD_ID);
    static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(Registries.ENTITY_TYPE, FlansMod.MOD_ID);
    static final DeferredRegister<BlockEntityType<?>> blockEntityRegistry = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FlansMod.MOD_ID);
    static final DeferredRegister<Enchantment> enchantmentRegistry = DeferredRegister.create(Registries.ENCHANTMENT, FlansMod.MOD_ID);

    static void register(IEventBus bus)
    {
        FlansModBlocks.initialize();
        FlansModItems.initialize();
        FlansModMenus.initialize();
        FlansModParticles.initialize();
        FlansModEntities.initialize();

        blockRegistry.register(bus);
        blockEntityRegistry.register(bus);
        itemRegistry.register(bus);
        particleRegistry.register(bus);
        soundEventRegistry.register(bus);
        creativeModeTabRegistry.register(bus);
        entityRegistry.register(bus);
        menuRegistry.register(bus);
        enchantmentRegistry.register(bus);
    }
}
