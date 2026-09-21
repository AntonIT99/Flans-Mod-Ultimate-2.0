package com.flansmodultimate.client.gui.options;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The config the options screen is editing. The client config is written locally; the common config belongs
 * to the server, which validates and applies the change and tells every client what came of it.
 */
public interface ConfigTarget
{
    ForgeConfigSpec spec();

    /** The value in force, which for the common config of a server is the server's, not this client's. */
    Object get(ForgeConfigSpec.ConfigValue<?> value);

    /** Applies a change the player made with a single click. */
    void set(ForgeConfigSpec.ConfigValue<?> value, Object newValue);

    /**
     * Records a change from a slider, which fires on every step of a drag. It is applied by
     * {@link #flush()} when the screen closes, so one drag does not write the config a hundred times.
     */
    void setWhileDragging(ForgeConfigSpec.ConfigValue<?> value, Object newValue);

    /** Applies everything {@link #setWhileDragging} recorded. */
    void flush();

    /** Whether this player may change these settings at all. */
    boolean editable();

    /** Whether the values are known yet; a server's common config takes a round trip to arrive. */
    default boolean ready()
    {
        return true;
    }
}
