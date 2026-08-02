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
        {
            FlansMod.log.warn("Could not parse particle options from string: '{}'", s);
            return;
        }

        Particle particle = Minecraft.getInstance().particleEngine.createParticle(opt.get(), x, y, z, vx, vy, vz);
        if (particle != null && scale != 1.0F)
            particle.scale(scale);
    }

    private static Optional<ParticleOptions> toOptions(String raw)
    {
        if (StringUtils.isBlank(raw))
            return Optional.empty();

        String s = raw.toLowerCase(Locale.ROOT);
        return toItemBlockParticleOptions(s).or(() -> toNamedOptions(s));
    }

    private static Optional<ParticleOptions> toItemBlockParticleOptions(String s)
    {
        // Item/Block patterns first: "iconcrack_modid:item", "blockcrack_modid:block", "blockdust_modid:block"
        if (s.contains(FlanParticles.ICON_CRACK + "_") || s.contains(FlanParticles.BLOCK_CRACK + "_") || s.contains(FlanParticles.BLOCK_DUST + "_"))
        {
            // keep the rest intact so IDs like "mod:item" work
            String[] split = s.split("_", 2);
            if (split.length > 1)
            {
                String kind = split[0];
                String id  = split[1];

                return switch (kind)
                {
                    case FlanParticles.ICON_CRACK -> ModUtils.getItemStack(id).map(stack -> new ItemParticleOption(ParticleTypes.ITEM, stack));
                    case FlanParticles.BLOCK_CRACK -> ModUtils.getBlockState(id).map(blockstate -> new BlockParticleOption(ParticleTypes.BLOCK, blockstate));
                    case FlanParticles.BLOCK_DUST -> ModUtils.getBlockState(id).map(blockstate -> new BlockParticleOption(ParticleTypes.FALLING_DUST, blockstate));
                    default -> Optional.empty();
                };
            }
        }

        return Optional.empty();
    }

    private static Optional<ParticleOptions> toNamedOptions(String s)
    {
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
            case FlanParticles.EXPLODE -> Optional.of(FlansMod.explodeParticle.get());
            case FlanParticles.RED_DUST -> Optional.of(new DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 1.0F));
            case FlanParticles.HUGE_EXPLOSION -> Optional.of(ParticleTypes.EXPLOSION_EMITTER);
            case FlanParticles.LARGE_EXPLODE -> Optional.of(ParticleTypes.EXPLOSION);
            case FlanParticles.FIREWORKS_SPARK -> Optional.of(ParticleTypes.FIREWORK);
            case FlanParticles.BUBBLE -> Optional.of(ParticleTypes.BUBBLE);
            case FlanParticles.SPLASH -> Optional.of(ParticleTypes.SPLASH);
            case FlanParticles.WAKE -> Optional.of(ParticleTypes.FISHING);
            case FlanParticles.DROP ->  Optional.of(ParticleTypes.FALLING_WATER); // or RAIN?
            case FlanParticles.DRIP_WATER -> Optional.of(ParticleTypes.DRIPPING_WATER);
            case FlanParticles.SUSPENDED -> Optional.of(ParticleTypes.UNDERWATER);
            case FlanParticles.DEPTH_SUSPEND -> Optional.of(ParticleTypes.UNDERWATER); // actually removed, no true equivalent
            case FlanParticles.TOWN_AURA -> Optional.of(ParticleTypes.ASH); // or MYCELIUM?
            case FlanParticles.CRIT -> Optional.of(ParticleTypes.CRIT);
            case FlanParticles.MAGIC_CRIT -> Optional.of(ParticleTypes.ENCHANTED_HIT);
            case FlanParticles.SMOKE -> Optional.of(ParticleTypes.SMOKE);
            case FlanParticles.LARGE_SMOKE -> Optional.of(ParticleTypes.LARGE_SMOKE);
            case FlanParticles.SPELL -> Optional.of(ParticleTypes.EFFECT);
            case FlanParticles.INSTANT_SPELL -> Optional.of(ParticleTypes.INSTANT_EFFECT);
            case FlanParticles.MOB_SPELL -> Optional.of(ParticleTypes.ENTITY_EFFECT);
            case FlanParticles.MOB_SPELL_AMBIENT -> Optional.of(ParticleTypes.AMBIENT_ENTITY_EFFECT);
            case FlanParticles.WITCH_MAGIC -> Optional.of(ParticleTypes.WITCH);
            case FlanParticles.DRIP_LAVA -> Optional.of(ParticleTypes.DRIPPING_LAVA);
            case FlanParticles.ANGRY_VILLAGER -> Optional.of(ParticleTypes.ANGRY_VILLAGER);
            case FlanParticles.HAPPY_VILLAGER -> Optional.of(ParticleTypes.HAPPY_VILLAGER);
            case FlanParticles.NOTE -> Optional.of(ParticleTypes.NOTE);
            case FlanParticles.PORTAL -> Optional.of(ParticleTypes.PORTAL);
            case FlanParticles.ENCHANTMENT_TABLE -> Optional.of(ParticleTypes.ENCHANT);
            case FlanParticles.FLAME -> Optional.of(ParticleTypes.FLAME);
            case FlanParticles.LAVA -> Optional.of(ParticleTypes.LAVA);
            case FlanParticles.CLOUD -> Optional.of(ParticleTypes.CLOUD);
            case FlanParticles.SNOWBALL_POOF -> Optional.of(ParticleTypes.ITEM_SNOWBALL);
            case FlanParticles.SNOW_SHOVEL -> Optional.of(ParticleTypes.POOF);
            case FlanParticles.SLIME -> Optional.of(ParticleTypes.ITEM_SLIME);
            case FlanParticles.HEART -> Optional.of(ParticleTypes.HEART);
            case FlanParticles.BARRIER -> Optional.of(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()));
            case FlanParticles.DROPLET -> Optional.of(ParticleTypes.FALLING_WATER);
            case FlanParticles.MOB_APPEARANCE -> Optional.of(ParticleTypes.ELDER_GUARDIAN);
            case FlanParticles.DRAGON_BREATH -> Optional.of(ParticleTypes.DRAGON_BREATH);
            case FlanParticles.END_ROD -> Optional.of(ParticleTypes.END_ROD);
            case FlanParticles.DAMAGE_INDICATOR -> Optional.of(ParticleTypes.DAMAGE_INDICATOR);
            case FlanParticles.SWEEP_ATTACK -> Optional.of(ParticleTypes.SWEEP_ATTACK);
            case FlanParticles.FALLING_DUST -> Optional.of(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()));
            case FlanParticles.SPIT -> Optional.of(ParticleTypes.SPIT);
            case FlanParticles.TOTEM -> Optional.of(ParticleTypes.TOTEM_OF_UNDYING);
            default -> toRegisteredParticleOptions(s);
        };
    }

    private static Optional<ParticleOptions> toRegisteredParticleOptions(String s)
    {
        // Registered particle IDs
        if (s.contains(":"))
        {
            return Optional.ofNullable(ResourceLocation.tryParse(s))
                .map(ForgeRegistries.PARTICLE_TYPES::getValue)
                .filter(ParticleOptions.class::isInstance)
                .map(ParticleOptions.class::cast);
        }

        return Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", s))
            .map(ForgeRegistries.PARTICLE_TYPES::getValue)
            .filter(ParticleOptions.class::isInstance)
            .map(ParticleOptions.class::cast);
    }
}
