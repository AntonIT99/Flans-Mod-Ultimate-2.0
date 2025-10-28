package com.wolffsarmormod.client.render;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleHelper
{
    // TODO: FMU Particles

    public static boolean spawnFromString(ClientLevel level, String s, double x, double y, double z)
    {
        return spawnFromString(level, s, x, y, z, 0.0, 0.0, 0.0);
    }

    public static boolean spawnFromString(ClientLevel level, String s, double x, double y, double z, double vx, double vy, double vz)
    {
        Optional<ParticleOptions> opt = toOptions(s);
        if (opt.isEmpty())
            return false;
        level.addParticle(opt.get(), x, y, z, vx, vy, vz);
        return true;
    }

    public static Optional<ParticleOptions> toOptions(String raw)
    {
        if (raw == null || raw.isEmpty())
            return Optional.empty();

        String s = raw.toLowerCase();

        // Item/block patterns first: "iconcrack_modid:item", "blockcrack_modid:block", "blockdust_modid:block"
        if (s.contains("_"))
        {
            // keep the rest intact so IDs like "mod:item" work
            String[] split = s.split("_", 2);
            if (split.length == 2)
            {
                String kind = split[0];
                String id = split[1]; // expected "modid:itemname" or "modid:blockname"

                if (kind.equals("iconcrack"))
                {
                    Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
                    if (item == null) return Optional.empty();
                    return Optional.of(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(item)));
                }

                if (kind.equals("blockcrack") || kind.equals("blockdust"))
                {
                    Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(id));
                    if (block == null) return Optional.empty();
                    // In modern MC both of these are typically represented with BLOCK particles.
                    return Optional.of(new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState()));
                }
            }
        }

        // Direct name mappings
        return switch (s)
        {
            case "hugeexplosion" -> Optional.of(ParticleTypes.EXPLOSION_EMITTER);
            case "largeexplode", "explode" -> Optional.of(ParticleTypes.EXPLOSION);
            case "fireworksspark" -> Optional.of(ParticleTypes.FIREWORK);
            case "bubble" -> Optional.of(ParticleTypes.BUBBLE);
            case "splash" -> Optional.of(ParticleTypes.SPLASH);
            case "wake" -> Optional.of(ParticleTypes.FISHING);
            case "drop" -> Optional.of(ParticleTypes.DRIPPING_WATER);
            case "suspended", "depthsuspend" -> Optional.of(ParticleTypes.MYCELIUM);
            case "townaura" -> Optional.of(ParticleTypes.ASH);
            case "crit" -> Optional.of(ParticleTypes.CRIT);
            case "magiccrit" -> Optional.of(ParticleTypes.ENCHANTED_HIT);
            case "smoke" -> Optional.of(ParticleTypes.SMOKE);
            case "largesmoke" -> Optional.of(ParticleTypes.LARGE_SMOKE);
            case "spell", "instantspell" -> Optional.of(ParticleTypes.INSTANT_EFFECT);
            case "mobspell" -> Optional.of(ParticleTypes.ENTITY_EFFECT);
            case "mobspellambient" -> Optional.of(ParticleTypes.AMBIENT_ENTITY_EFFECT);
            case "witchmagic" -> Optional.of(ParticleTypes.WITCH);
            case "dripwater" -> Optional.of(ParticleTypes.DRIPPING_WATER);
            case "driplava" -> Optional.of(ParticleTypes.DRIPPING_LAVA);
            case "angryvillager" -> Optional.of(ParticleTypes.ANGRY_VILLAGER);
            case "happyvillager" -> Optional.of(ParticleTypes.HAPPY_VILLAGER);
            case "note" -> Optional.of(ParticleTypes.NOTE);
            case "portal" -> Optional.of(ParticleTypes.PORTAL);
            case "enchantmenttable" -> Optional.of(ParticleTypes.ENCHANT);
            case "flame" -> Optional.of(ParticleTypes.FLAME);
            case "lava" -> Optional.of(ParticleTypes.LAVA);
            case "footstep" -> Optional.of(ParticleTypes.CLOUD);
            case "cloud" -> Optional.of(ParticleTypes.CLOUD);
            case "reddust" -> Optional.of(new DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 1.0F));
            case "snowballpoof" -> Optional.of(ParticleTypes.ITEM_SNOWBALL);
            case "snowshovel" -> Optional.of(ParticleTypes.POOF);
            case "slime" -> Optional.of(ParticleTypes.ITEM_SLIME);
            case "heart" -> Optional.of(ParticleTypes.HEART);
            case "barrier" -> Optional.of(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()));
            default -> Optional.empty();
        };
    }
}
