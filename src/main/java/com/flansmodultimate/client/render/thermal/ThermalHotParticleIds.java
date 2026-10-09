package com.flansmodultimate.client.render.thermal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Which particle type ids look hot in thermal sights: the Labjac Edition's list under modern ids, any type whose
 * name mentions fire, explosions, flashes, sparks and the like (but not smoke), and the configured extras.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ThermalHotParticleIds
{
    static final Set<String> HOT_TYPE_IDS = Set.of("minecraft:explosion", "minecraft:explosion_emitter", "minecraft:flame", "minecraft:small_flame", "minecraft:soul_fire_flame", "minecraft:lava",
        "minecraft:dripping_lava", "minecraft:falling_lava", "minecraft:landing_lava", "minecraft:firework", "minecraft:flash", "minecraft:electric_spark", "flansmodultimate:afterburn",
        "flansmodultimate:blast_puff", "flansmodultimate:big_smoke", "flansmodultimate:debris_1", "flansmodultimate:explode", "flansmodultimate:fire_explosion", "flansmodultimate:flare",
        "flansmodultimate:flash", "flansmodultimate:fm_flame", "flansmodultimate:fm_muzzle_flash", "flansmodultimate:fm_smoke", "flansmodultimate:fm_tracer", "flansmodultimate:fm_tracer_green",
        "flansmodultimate:fm_tracer_red", "flansmodultimate:rocket_exhaust", "flansmodultimate:smoke_burst");
    static final String[] HOT_KEYWORDS = {"explosion", "explode", "flame", "fire", "flash", "muzzle", "spark", "lava", "plasma", "nuke", "napalm", "thermite", "fireball", "exhaust", "afterburn",
        "flare", "tracer", "incendiary"};

    /**
     * @param id
     *            a particle type id such as {@code minecraft:flame}
     * @param configured
     *            extra ids from the client config; an entry without a namespace matches any namespace
     */
    static boolean isHot(String id, List<String> configured)
    {
        String lower = id.toLowerCase(Locale.ROOT);
        String path = lower.substring(lower.indexOf(':') + 1);
        if (HOT_TYPE_IDS.contains(lower))
            return true;
        for (String entry : configured)
        {
            String extra = entry.toLowerCase(Locale.ROOT);
            if (extra.equals(lower) || extra.indexOf(':') < 0 && extra.equals(path))
                return true;
        }
        if (path.contains("smoke"))
            return false;
        for (String keyword : HOT_KEYWORDS)
            if (path.contains(keyword))
                return true;
        return false;
    }
}
