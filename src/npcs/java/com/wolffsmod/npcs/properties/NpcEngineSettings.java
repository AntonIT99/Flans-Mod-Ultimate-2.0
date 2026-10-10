package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EngineSound;
import com.flansmodultimate.api.FlansEntitySounds;
import lombok.Getter;
import lombok.Setter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** One custom engine channel; blank explicitly selects silence, rather than reverting to the model. */
@Getter
public final class NpcEngineSettings
{
    private static final String RANGE_KEY = "Range";
    private static final String REPEAT_TICKS = "RepeatTicks";

    private String sound = "";
    private int range = 50;
    @Setter
    private boolean overrideRepeat;
    private int repeatTicks = 20;
    private boolean initialized;

    public void setSound(String value)
    {
        String text = value.trim();
        ResourceLocation id = ResourceLocation.tryParse(text.contains(":") ? text : "flansmod:" + text);
        sound = !text.isBlank() && text.length() <= 256 && id != null ? id.toString() : "";
    }

    public void setRange(int value)
    {
        range = Math.max(1, Math.min(4096, value));
    }

    public void setRepeatTicks(int value)
    {
        repeatTicks = Math.max(1, Math.min(12000, value));
    }

    public void initialize(Optional<EngineSound> defaults, float serverRange)
    {
        if (initialized)
            return;
        setSound(defaults.map(EngineSound::sound).orElse(""));
        setRange(Math.round(serverRange));
        setRepeatTicks(defaults.map(EngineSound::repeatTicks).orElse(20));
        initialized = true;
    }

    public boolean automaticRepeat()
    {
        return !overrideRepeat && sound.startsWith("flansmod:");
    }

    public void resetToVehicle(Optional<EngineSound> defaults, float serverRange)
    {
        initialized = false;
        overrideRepeat = false;
        initialize(defaults, defaults.map(EngineSound::range).orElse(serverRange));
    }

    public int effectiveRepeatTicks()
    {
        return automaticRepeat() ? FlansEntitySounds.getSoundLength(ResourceLocation.parse(sound)).orElse(repeatTicks) : repeatTicks;
    }

    public Optional<EngineSound> resolve()
    {
        return sound.isBlank() ? Optional.empty() : Optional.of(new EngineSound(sound, range, effectiveRepeatTicks()));
    }

    public void load(CompoundTag tag)
    {
        initialized = tag.getBoolean("Initialized");
        setSound(tag.getString("Sound"));
        setRange(tag.contains(RANGE_KEY) ? tag.getInt(RANGE_KEY) : 50);
        overrideRepeat = tag.getBoolean("OverrideRepeat");
        setRepeatTicks(tag.contains(REPEAT_TICKS) ? tag.getInt(REPEAT_TICKS) : 20);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Initialized", initialized);
        tag.putString("Sound", sound);
        tag.putInt(RANGE_KEY, range);
        tag.putBoolean("OverrideRepeat", overrideRepeat);
        tag.putInt(REPEAT_TICKS, repeatTicks);
        return tag;
    }
}
