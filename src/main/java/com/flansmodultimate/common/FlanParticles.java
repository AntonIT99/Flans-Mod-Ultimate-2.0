package com.flansmodultimate.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanParticles
{
    // Flan's Mod Custom Particles
    public static final String FM_AFTERBURN = "flansmod.afterburn";
    public static final String FM_BIG_SMOKE = "flansmod.bigsmoke";
    public static final String FM_DEBRIS_1 = "flansmod.debris1";
    public static final String FM_FLARE = "flansmod.flare";
    public static final String FM_FLASH = "flansmod.flash";
    public static final String FM_FLAME = "flansmod.fmflame";
    public static final String FM_TRACER = "flansmod.fmtracer";
    public static final String FM_TRACER_GREEN = "flansmod.fmtracergreen";
    public static final String FM_TRACER_RED = "flansmod.fmtracerred";
    public static final String FM_MUZZLE_FLASH = "flansmod.muzzleflash";
    public static final String FM_ROCKET_EXHAUST = "flansmod.rocketexhaust";
    public static final String FM_SMOKE = "flansmod.fmsmoke";
    public static final String FM_SMOKE_BURST = "flansmod.smokeburst";
    public static final String FM_SMOKER = "flansmod.smoker";
    public static final String FM_SMOKER_1 = "flansmod.smoker1";

    // Minecraft Legacy Particles
    public static final String RED_DUST = "reddust";
    public static final String HUGE_EXPLOSION = "hugeexplosion";
    public static final String LARGE_EXPLODE = "largeexplode";
    public static final String EXPLODE = "explode";
    public static final String FIREWORKS_SPARK = "fireworksspark";
    public static final String BUBBLE = "bubble";
    public static final String SPLASH = "splash";
    public static final String WAKE = "wake";
    public static final String DROP = "drop";
    public static final String DRIP_WATER = "dripwater";
    public static final String SUSPENDED = "suspended";
    public static final String DEPTH_SUSPEND = "depthsuspend";
    public static final String TOWN_AURA = "townaura";
    public static final String CRIT = "crit";
    public static final String MAGIC_CRIT = "magiccrit";
    public static final String SMOKE = "smoke";
    public static final String LARGE_SMOKE = "largesmoke";
    public static final String SPELL = "spell";
    public static final String INSTANT_SPELL = "instantspell";
    public static final String MOB_SPELL = "mobspell";
    public static final String MOB_SPELL_AMBIENT = "mobspellambient";
    public static final String WITCH_MAGIC = "witchmagic";
    public static final String DRIP_LAVA = "driplava";
    public static final String ANGRY_VILLAGER = "angryvillager";
    public static final String HAPPY_VILLAGER = "happyvillager";
    public static final String NOTE = "note";
    public static final String PORTAL = "portal";
    public static final String ENCHANTMENT_TABLE = "enchantmenttable";
    public static final String FLAME = "flame";
    public static final String LAVA = "lava";
    public static final String CLOUD = "cloud";
    public static final String SNOWBALL_POOF = "snowballpoof";
    public static final String SNOW_SHOVEL = "snowshovel";
    public static final String SLIME = "slime";
    public static final String HEART = "heart";
    public static final String BARRIER = "barrier";
    public static final String DROPLET = "droplet";
    public static final String MOB_APPEARANCE = "mobappearance";
    public static final String DRAGON_BREATH = "dragonbreath";
    public static final String END_ROD = "endrod";
    public static final String DAMAGE_INDICATOR = "damageindicator";
    public static final String SWEEP_ATTACK = "sweepattack";
    public static final String FALLING_DUST = "fallingdust";
    public static final String SPIT = "spit";
    public static final String TOTEM = "totem";
    public static final String ICON_CRACK = "iconcrack";
    public static final String BLOCK_CRACK = "blockcrack";
    public static final String BLOCK_DUST = "blockdust";

    /** Particles of this mod, named "flansmod.<name>" in content packs. */
    private static final String FLANSMOD_PREFIX = "flansmod.";
    /** These take a block or item id after an underscore, e.g. "blockcrack_minecraft:stone". */
    private static final List<String> RESOURCE_PARTICLES = List.of(ICON_CRACK, BLOCK_CRACK, BLOCK_DUST);

    private static final Set<String> NAMES = Arrays.stream(FlanParticles.class.getFields())
        .filter(field -> field.getType() == String.class && Modifier.isStatic(field.getModifiers()))
        .map(FlanParticles::readName)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());

    /** Short forms of this mod's particles: "flare" for "flansmod.flare". */
    private static final Map<String, String> SHORT_NAMES = NAMES.stream()
        .filter(name -> name.startsWith(FLANSMOD_PREFIX))
        .collect(Collectors.toUnmodifiableMap(name -> name.substring(FLANSMOD_PREFIX.length()), name -> name));

    @Nullable
    private static String readName(Field field)
    {
        try
        {
            return (String) field.get(null);
        }
        catch (IllegalAccessException ignored)
        {
            return null;
        }
    }

    /**
     * Resolves a particle name as typed. The full name always wins, so a short form only
     * applies when it is not already a particle of its own: "flare" becomes "flansmod.flare",
     * while "flame" stays the vanilla particle rather than becoming "flansmod.fmflame".
     *
     * @return the canonical name, or empty when nothing of that name exists
     */
    public static Optional<String> resolve(@Nullable String raw)
    {
        if (raw == null || raw.isBlank())
            return Optional.empty();

        String name = raw.trim().toLowerCase(Locale.ROOT);
        if (NAMES.contains(name))
            return Optional.of(name);

        String shortName = SHORT_NAMES.get(name);
        if (shortName != null)
            return Optional.of(shortName);

        // Block and item variants carry their resource id in the name itself
        for (String kind : RESOURCE_PARTICLES)
        {
            if (name.startsWith(kind + "_") && name.length() > kind.length() + 1)
                return Optional.of(name);
        }
        return Optional.empty();
    }

    /** Every name the resolver accepts without a resource id, for command completion. */
    public static Stream<String> suggestions()
    {
        return Stream.concat(NAMES.stream(), SHORT_NAMES.keySet().stream()).sorted();
    }
}
