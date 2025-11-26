package com.flansmodultimate.client.render;

import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;

import java.util.Locale;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleHelper
{
    public static final String RED_DUST = "reddust";

    public static final String FM_DEBRIS_1 = "flansmod.debris1";
    public static final String FM_FLARE = "flansmod.flare";

    public static final String ICON_CRACK = "iconcrack";
    public static final String BLOCK_CRACK = "blockcrack";
    public static final String BLOCK_DUST = "blockdust";

    //TODO: Particle size?
    //TODO: FMU Particles

    public static void spawnFromString(ClientLevel level, String s, double x, double y, double z)
    {
        spawnFromString(level, s, x, y, z, 0, 0, 0);
    }

    public static void spawnFromString(ClientLevel level, String s, double x, double y, double z, double vx, double vy, double vz)
    {
        Optional<ParticleOptions> opt = toOptions(s);
        if (opt.isEmpty())
            return;
        level.addParticle(opt.get(), x, y, z, vx, vy, vz);
    }

    private static Optional<ParticleOptions> toOptions(String raw)
    {
        if (StringUtils.isBlank(raw))
            return Optional.empty();

        String s = raw.toLowerCase(Locale.ROOT);

        // Item/block patterns first: "iconcrack_modid:item", "blockcrack_modid:block", "blockdust_modid:block"
        if (s.contains("_"))
        {
            // keep the rest intact so IDs like "mod:item" work
            String[] split = s.split("_", 2);
            if (split.length > 1)
            {
                String kind = split[0];
                String id = split[1]; // expected "modid:itemname" or "modid:blockname"

                return switch (kind)
                {
                    case ICON_CRACK -> ModUtils.getItemStack(id).map(stack -> new ItemParticleOption(ParticleTypes.ITEM, stack));
                    case BLOCK_CRACK -> ModUtils.getBlockState(id).map(blockstate -> new BlockParticleOption(ParticleTypes.BLOCK, blockstate));
                    case BLOCK_DUST -> ModUtils.getBlockState(id).map(blockstate -> new BlockParticleOption(ParticleTypes.FALLING_DUST, blockstate));
                    default -> Optional.empty();
                };
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
            case "drop", "dripwater" -> Optional.of(ParticleTypes.DRIPPING_WATER);
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
            case RED_DUST -> Optional.of(new DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 1.0F));
            case "snowballpoof" -> Optional.of(ParticleTypes.ITEM_SNOWBALL);
            case "snowshovel" -> Optional.of(ParticleTypes.POOF);
            case "slime" -> Optional.of(ParticleTypes.ITEM_SLIME);
            case "heart" -> Optional.of(ParticleTypes.HEART);
            case "barrier" -> Optional.of(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()));
            default -> Optional.empty(); //TODO: Get by its register Name?
        };
    }
}
