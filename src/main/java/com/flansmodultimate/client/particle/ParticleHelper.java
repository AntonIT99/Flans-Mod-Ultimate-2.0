package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleHelper
{
    public static void spawnFromString(String s, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        String normalized = normalize(s);
        if (normalized == null)
        {
            warnCouldNotParse(s);
            return;
        }

        Optional<LegacyResourceRequest> legacyRequest = LegacyResourceRequest.parse(normalized, false);
        if (legacyRequest.isPresent())
        {
            if (!spawnLegacyResourceParticle(legacyRequest.get(), BlockPos.containing(x, y, z), x, y, z, vx, vy, vz, scale))
                warnCouldNotParse(s);
            return;
        }

        Optional<ParticleOptions> opt = toNamedOptions(normalized);
        if (opt.isEmpty())
        {
            warnCouldNotParse(s);
            return;
        }

        Particle particle = Minecraft.getInstance().particleEngine.createParticle(opt.get(), x, y, z, vx, vy, vz);
        scaleParticle(particle, scale);
    }

    public static void spawnFromString(String s, BlockState state, BlockPos sourcePos,
                                       double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        String normalized = normalize(s);
        if (normalized == null)
        {
            warnCouldNotParse(s);
            return;
        }

        Optional<LegacyResourceRequest> request = LegacyResourceRequest.parse(normalized, true);
        if (request.isEmpty() || request.get().kind() == LegacyResourceKind.ICON_CRACK)
        {
            warnCouldNotParse(s);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;

        LegacyBlockParticle.Variant variant = request.get().kind() == LegacyResourceKind.BLOCK_DUST ? LegacyBlockParticle.Variant.DUST : LegacyBlockParticle.Variant.CRACK;
        addParticle(LegacyBlockParticle.create(minecraft.level, state, sourcePos, variant, x, y, z, vx, vy, vz), scale);
    }

    private static boolean spawnLegacyResourceParticle(LegacyResourceRequest request, BlockPos sourcePos, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return true;

        ClientLevel level = minecraft.level;
        switch (request.kind())
        {
            case ICON_CRACK:
            {
                Optional<ItemStack> stack = getLegacyItemStack(request.resourceId());
                if (stack.isEmpty())
                    return false;
                addParticle(new LegacyItemParticle(level, stack.get(), x, y, z, vx, vy, vz), scale);
                return true;
            }
            case BLOCK_CRACK, BLOCK_DUST:
            {
                Optional<BlockState> state = ModUtils.getBlockState(request.resourceId());
                if (state.isEmpty())
                    return false;
                LegacyBlockParticle.Variant variant = request.kind() == LegacyResourceKind.BLOCK_DUST ? LegacyBlockParticle.Variant.DUST : LegacyBlockParticle.Variant.CRACK;
                addParticle(LegacyBlockParticle.create(level, state.get(), sourcePos, variant, x, y, z, vx, vy, vz), scale);
                return true;
            }
        }
        return false;
    }

    private static void addParticle(Particle particle, float scale)
    {
        if (particle == null)
            return;
        scaleParticle(particle, scale);
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static void scaleParticle(Particle particle, float scale)
    {
        if (particle != null && scale != 1.0F)
            particle.scale(scale);
    }

    private static Optional<ItemStack> getLegacyItemStack(String resourceId)
    {
        Optional<ItemStack> exact = ModUtils.getItemStack(resourceId);
        if (exact.isPresent())
            return exact;

        // Legacy syntax optionally appended a numeric damage/metadata value.
        // Trying the complete ID first keeps modern IDs containing underscores
        // unambiguous (for example minecraft:iron_sword).
        int separator = resourceId.lastIndexOf('_');
        if (separator <= 0 || separator == resourceId.length() - 1)
            return Optional.empty();

        try
        {
            int damage = Integer.parseInt(resourceId.substring(separator + 1));
            return ModUtils.getItemStack(resourceId.substring(0, separator)).map(stack ->
            {
                stack.setDamageValue(damage);
                return stack;
            });
        }
        catch (NumberFormatException ignored)
        {
            return Optional.empty();
        }
    }

    private static String normalize(String raw)
    {
        return raw == null || raw.isBlank() ? null : raw.toLowerCase(Locale.ROOT);
    }

    private static void warnCouldNotParse(String s)
    {
        FlansMod.log.warn("Could not parse particle options from string: '{}'", s);
    }

    private enum LegacyResourceKind
    {
        ICON_CRACK(FlanParticles.ICON_CRACK),
        BLOCK_CRACK(FlanParticles.BLOCK_CRACK),
        BLOCK_DUST(FlanParticles.BLOCK_DUST);

        private final String commandName;

        LegacyResourceKind(String commandName)
        {
            this.commandName = commandName;
        }
    }

    private record LegacyResourceRequest(LegacyResourceKind kind, String resourceId)
    {
        private static Optional<LegacyResourceRequest> parse(String s, boolean allowBareName)
        {
            for (LegacyResourceKind kind : LegacyResourceKind.values())
            {
                if (allowBareName && s.equals(kind.commandName))
                    return Optional.of(new LegacyResourceRequest(kind, ""));

                String prefix = kind.commandName + "_";
                if (s.startsWith(prefix) && s.length() > prefix.length())
                    return Optional.of(new LegacyResourceRequest(kind, s.substring(prefix.length())));
            }
            return Optional.empty();
        }
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
            case FlanParticles.FM_SMOKER, FlanParticles.FM_SMOKER_1 -> Optional.of(FlansMod.smokeGrenadeParticle.get());
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
