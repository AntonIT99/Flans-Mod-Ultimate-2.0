package com.flansmodultimate.client.input;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Decides which shared keys the controls screen should stop calling a conflict.
 *
 * <p>Two mappings only really clash if a player can reach both at the same
 * moment. Forge cannot express that on its own: {@code KeyMapping.same} ends
 * with a bare key comparison whenever two conflict contexts do not conflict, and
 * every vanilla mapping is {@link KeyConflictContext#UNIVERSAL}, which conflicts
 * with everything. So the flight axes on W/A/S/D would be drawn red against both
 * the ground vehicle binds and vanilla walking, neither of which a pilot can use
 * while flying.</p>
 *
 * <p>A pair is hidden when it cannot bite: the two are never live at the same
 * moment, or the action being displaced is one this mod deliberately takes over
 * while the player is at the controls. Anything a player would actually want to
 * know about still shows up.</p>
 *
 * <p>This is presentation only. {@code KeyMapping.same} has exactly one caller
 * in the game, the row colouring in {@code KeyBindsList}, so nothing here can
 * change what a key does.</p>
 */
@OnlyIn(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyConflictFilter
{
    private static final String OURS_PREFIX = "key." + FlansMod.MOD_ID + ".";
    /**
     * Flan's Mod Reloaded, matched by the strings it registers rather than by
     * its classes, so this stays a soft dependency: with the mod absent nothing
     * ever matches and the code still runs.
     */
    private static final String RELOADED_CATEGORY = "key.categories.flansmod";
    private static final String RELOADED_PREFIX = "key.flansmod.";

    /** True when the controls screen has nothing worth reporting about this pair. */
    public static boolean cannotOverlap(KeyMapping one, KeyMapping other)
    {
        boolean oneIsOurs = isOurs(one);
        boolean otherIsOurs = isOurs(other);

        // Never change how two mappings we know nothing about are reported.
        if (!oneIsOurs && !otherIsOurs)
            return false;
        if (oneIsOurs && otherIsOurs)
            return bothOurs(one, other);

        return againstOutsider(oneIsOurs ? one : other, oneIsOurs ? other : one);
    }

    private static boolean bothOurs(KeyMapping one, KeyMapping other)
    {
        if (isDriveableBind(one) && isDriveableBind(other))
        {
            // Both sides declare when they are live, so believe them. This is
            // the pairing Forge's fallback gets wrong, and it is what lets the
            // flight controls and the driving controls share keys.
            IKeyConflictContext oneContext = one.getKeyConflictContext();
            IKeyConflictContext otherContext = other.getKeyConflictContext();
            return !oneContext.conflicts(otherContext) && !otherContext.conflicts(oneContext);
        }
        // checkKeys reads the gun binds only in the branch it takes when the
        // player is at the controls of nothing, so those two never overlap.
        return isDriveableBind(one) && KeyInputHandler.isOnFootOnly(other)
            || isDriveableBind(other) && KeyInputHandler.isOnFootOnly(one);
    }

    private static boolean againstOutsider(KeyMapping ours, KeyMapping outsider)
    {
        // Flan's Mod Reloaded drives its own vehicles and its own weapons. Our
        // driveable controls only answer while at the controls of one of ours,
        // and our gun binds only while holding one of ours, so either way both
        // mods can act on the same key without stepping on each other.
        if (isFlansModReloaded(outsider))
            return isDriveableBind(ours) || KeyInputHandler.isOnFootOnly(ours);

        // Everything below is about giving up a vanilla action to a driveable.
        // A bind of ours that is live on foot has no such claim to make.
        if (!isDriveableBind(ours))
            return false;

        return isInertWhileRiding(outsider) || KeyInputHandler.isClaimableVanillaAction(outsider);
    }

    private static boolean isOurs(KeyMapping mapping)
    {
        return mapping.getName().startsWith(OURS_PREFIX);
    }

    private static boolean isDriveableBind(KeyMapping mapping)
    {
        return mapping.getKeyConflictContext() instanceof EnumKeyConflictContext;
    }

    private static boolean isFlansModReloaded(KeyMapping mapping)
    {
        return RELOADED_CATEGORY.equals(mapping.getCategory())
            || mapping.getName().startsWith(RELOADED_PREFIX);
    }

    /**
     * Vanilla controls that do nothing once the player is a passenger. Sneak is
     * deliberately absent: it still dismounts. So are attack and use, which
     * still fire a driveable's weapons.
     */
    private static boolean isInertWhileRiding(KeyMapping mapping)
    {
        Options options = Minecraft.getInstance().options;
        return mapping == options.keyUp || mapping == options.keyDown
            || mapping == options.keyLeft || mapping == options.keyRight
            || mapping == options.keyJump || mapping == options.keySprint;
    }
}
