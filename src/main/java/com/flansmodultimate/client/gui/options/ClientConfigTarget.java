package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ModClientConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;

/** The client's own config: written straight to its file, with the changes taking effect immediately. */
public class ClientConfigTarget implements ConfigTarget
{
    @Override
    public ForgeConfigSpec spec()
    {
        return ModClientConfig.configSpec;
    }

    @Override
    public Object get(ConfigValue<?> value)
    {
        return value.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(ConfigValue<?> value, Object newValue)
    {
        ModClientConfig.setAndSave((ConfigValue<Object>) value, newValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setWhileDragging(ConfigValue<?> value, Object newValue)
    {
        ModClientConfig.set((ConfigValue<Object>) value, newValue);
    }

    @Override
    public void flush()
    {
        ModClientConfig.flushPendingChanges();
    }

    @Override
    public boolean editable()
    {
        return true;
    }
}
