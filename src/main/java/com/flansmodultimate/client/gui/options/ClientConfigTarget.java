package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ModClientConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The client's own config: written straight to its file, with the changes taking effect immediately. */
public class ClientConfigTarget implements ConfigTarget
{
    @Override
    public ModConfigSpec spec()
    {
        return ModClientConfig.configSpec;
    }

    @Override
    public Object get(ModConfigSpec.ConfigValue<?> value)
    {
        return value.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(ModConfigSpec.ConfigValue<?> value, Object newValue)
    {
        ModClientConfig.setAndSave((ModConfigSpec.ConfigValue<Object>) value, newValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setWhileDragging(ModConfigSpec.ConfigValue<?> value, Object newValue)
    {
        ModClientConfig.set((ModConfigSpec.ConfigValue<Object>) value, newValue);
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
