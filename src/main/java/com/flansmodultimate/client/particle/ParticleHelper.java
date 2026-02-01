package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.Locale;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleHelper
{
    public static void spawnFromString(String s, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        Optional<ParticleOptions> opt = toOptions(s);
        if (opt.isEmpty())
            return;

        Particle particle = Minecraft.getInstance().particleEngine.createParticle(opt.get(), x, y, z, vx, vy, vz);
        if (particle != null && scale != 1.0F)
            particle.scale(scale);
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
                    //TODO: implement custom particles with physics from 1.7.10
                    case FlanParticles.ICON_CRACK -> ModUtils.getItemStack(id).map(stack -> new ItemParticleOption(ParticleTypes.ITEM, stack));
                    case FlanParticles.BLOCK_CRACK -> ModUtils.getBlockState(id).map(blockstate -> new BlockParticleOption(ParticleTypes.BLOCK, blockstate));
                    case FlanParticles.BLOCK_DUST -> ModUtils.getBlockState(id).map(blockstate -> new BlockParticleOption(ParticleTypes.FALLING_DUST, blockstate));
                    default -> Optional.empty();
                };
            }
        }

        // Direct name mappings
        return switch (s)
        {
            case FlanParticles.FM_AFTERBURN -> Optional.of(FlansMod.afterburnParticle.get());
            case FlanParticles.FM_BIG_SMOKE -> Optional.of(FlansMod.bigSmokeParticle.get());
            case FlanParticles.FM_DEBRIS_1 -> Optional.of(FlansMod.debris1Particle.get());
            case FlanParticles.FM_FLARE -> Optional.of(FlansMod.flareParticle.get());
            case FlanParticles.FM_FLASH -> Optional.of(FlansMod.flashParticle.get());
            case FlanParticles.FM_FLAME -> Optional.of(FlansMod.fmFlameParticle.get());
            case FlanParticles.FM_TRACER -> Optional.of(FlansMod.fmTracerParticle.get());
            case FlanParticles.FM_TRACER_GREEN -> Optional.of(FlansMod.fmTracerGreenParticle.get());
            case FlanParticles.FM_TRACER_RED -> Optional.of(FlansMod.fmTracerRedParticle.get());
            case FlanParticles.FM_MUZZLE_FLASH -> Optional.of(FlansMod.fmMuzzleFlashParticle.get());
            case FlanParticles.FM_ROCKET_EXHAUST -> Optional.of(FlansMod.rocketExhaustParticle.get());
            case FlanParticles.FM_SMOKE -> Optional.of(FlansMod.fmSmokeParticle.get());
            case FlanParticles.FM_SMOKE_BURST -> Optional.of(FlansMod.smokeBurstParticle.get());
            case FlanParticles.FM_SMOKER -> Optional.of(FlansMod.smokeGrenadeParticle.get());
            case FlanParticles.RED_DUST -> Optional.of(new DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 1.0F));
            case "hugeexplosion" -> Optional.of(ParticleTypes.EXPLOSION_EMITTER);
            case "largeexplode", "explode" -> Optional.of(ParticleTypes.EXPLOSION);
            case "fireworksspark" -> Optional.of(ParticleTypes.FIREWORK);
            case "bubble" -> Optional.of(ParticleTypes.BUBBLE);
            case "splash" -> Optional.of(ParticleTypes.SPLASH);
            case "wake" -> Optional.of(ParticleTypes.FISHING);
            case "drop" ->  Optional.of(ParticleTypes.FALLING_WATER);
            case "dripwater" -> Optional.of(ParticleTypes.DRIPPING_WATER);
            case "suspended" -> Optional.of(ParticleTypes.MYCELIUM);
            case "depthsuspend" -> Optional.of(ParticleTypes.UNDERWATER);
            case "townaura" -> Optional.of(ParticleTypes.ASH);
            case "crit" -> Optional.of(ParticleTypes.CRIT);
            case "magiccrit" -> Optional.of(ParticleTypes.ENCHANTED_HIT);
            case "smoke" -> Optional.of(ParticleTypes.SMOKE);
            case "largesmoke" -> Optional.of(ParticleTypes.LARGE_SMOKE);
            case "spell" -> Optional.of(ParticleTypes.EFFECT);
            case "instantspell" -> Optional.of(ParticleTypes.INSTANT_EFFECT);
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
            case "cloud" -> Optional.of(ParticleTypes.CLOUD);
            case "snowballpoof" -> Optional.of(ParticleTypes.ITEM_SNOWBALL);
            case "snowshovel" -> Optional.of(ParticleTypes.POOF);
            case "slime" -> Optional.of(ParticleTypes.ITEM_SLIME);
            case "heart" -> Optional.of(ParticleTypes.HEART);
            case "barrier" -> Optional.of(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()));
            default -> {
                if (!s.contains(":"))
                    yield Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", s))
                        .map(ForgeRegistries.PARTICLE_TYPES::getValue)
                        .filter(SimpleParticleType.class::isInstance)
                        .map(SimpleParticleType.class::cast);
                else
                    yield Optional.ofNullable(ResourceLocation.tryParse(s))
                        .map(ForgeRegistries.PARTICLE_TYPES::getValue)
                        .filter(SimpleParticleType.class::isInstance)
                        .map(SimpleParticleType.class::cast);
            }
        };
    }
}
