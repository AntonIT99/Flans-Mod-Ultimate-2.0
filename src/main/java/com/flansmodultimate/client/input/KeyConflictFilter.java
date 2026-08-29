package com.flansmodultimate.client.input;

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
 * <p>Only pairs where nothing is lost are hidden: either the two can never be
 * live together, or the displaced action has a bind of its own to reach it by.
 * A key that genuinely costs the player something still shows up.</p>
 *
 * <p>This is presentation only. {@code KeyMapping.same} has exactly one caller
 * in the game, the row colouring in {@code KeyBindsList}, so nothing here can
 * change what a key does.</p>
 */
@OnlyIn(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyConflictFilter
{
    /** True when these two mappings can never be live at the same time. */
    public static boolean cannotOverlap(KeyMapping one, KeyMapping other)
    {
        IKeyConflictContext oneContext = one.getKeyConflictContext();
        IKeyConflictContext otherContext = other.getKeyConflictContext();
        boolean oneIsOurs = oneContext instanceof EnumKeyConflictContext;
        boolean otherIsOurs = otherContext instanceof EnumKeyConflictContext;

        // Never change how two unrelated mods' bindings are reported.
        if (!oneIsOurs && !otherIsOurs)
            return false;

        if (oneIsOurs && otherIsOurs)
        {
            // Both sides declare when they are live, so believe them. This is
            // the pairing Forge's fallback gets wrong, and it is what lets the
            // flight axes and the driving controls share W/A/S/D.
            return !oneContext.conflicts(otherContext) && !otherContext.conflicts(oneContext);
        }

        // The other side is a mapping that claims to be live everywhere: either
        // a vanilla one, which is universal, or one of ours that is in-game.
        // Only the ones a rider cannot reach anyway are safe to hide.
        KeyMapping outsider = oneIsOurs ? other : one;
        IKeyConflictContext ourContext = oneIsOurs ? oneContext : otherContext;
        return isInertWhileRiding(outsider)
            || KeyInputHandler.isOnFootOnly(outsider)
            || ourContext == EnumKeyConflictContext.PLANE && isFlownByAPlaneBind(outsider);
    }

    /**
     * Vanilla actions a pilot reaches through a plane bind instead. A cockpit
     * has its own inventory and drop binds, so whichever key a flight control
     * takes, nothing is lost and there is nothing to warn about.
     *
     * <p>Deliberately limited to the plane context. In a ground vehicle these
     * keys still do their vanilla job and have no stand-in, so a driving bind
     * landing on one of them is a real clash and stays reported.</p>
     */
    private static boolean isFlownByAPlaneBind(KeyMapping mapping)
    {
        Options options = Minecraft.getInstance().options;
        return mapping == options.keyInventory || mapping == options.keyDrop;
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
