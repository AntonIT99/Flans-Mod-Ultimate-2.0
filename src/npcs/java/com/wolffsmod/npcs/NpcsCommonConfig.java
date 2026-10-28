package com.wolffsmod.npcs;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The module's common configuration, {@code wolffsmodnpcs-common.toml}: the defaults every world starts from, which operators
 * can override per world and per faction with commands.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcsCommonConfig
{
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue NPC_FRIENDLY_FIRE;
    private static final ForgeConfigSpec.BooleanValue PLAYER_FRIENDLY_FIRE;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("Friendly Fire");
        NPC_FRIENDLY_FIRE = builder.comment("Whether NPCs hurt NPCs of their own or an allied faction.",
            "Teams rounds follow their game type's friendlyfire variable instead. Override it per world or per faction with /flansnpcfriendlyfire.").define("npcFriendlyFire", false);
        PLAYER_FRIENDLY_FIRE = builder.comment("Whether NPCs and the players their faction is friendly to hurt each other.",
            "Teams rounds follow their game type's friendlyfire variable instead. Override it per world or per faction with /flansnpcfriendlyfire.").define("playerFriendlyFire", false);
        builder.pop();
        SPEC = builder.build();
    }

    /** @return whether NPCs hurt NPCs of their own or an allied faction, unless a world or faction says otherwise */
    public static boolean npcFriendlyFire()
    {
        return read(NPC_FRIENDLY_FIRE);
    }

    /** @return whether NPCs and the players their faction is friendly to hurt each other, unless a world or faction says otherwise */
    public static boolean playerFriendlyFire()
    {
        return read(PLAYER_FRIENDLY_FIRE);
    }

    /** Reads a value, or its default before the configuration is loaded. */
    private static boolean read(ForgeConfigSpec.BooleanValue value)
    {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
