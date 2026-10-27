package com.wolffsmod.npcs.names;

import com.flansmodultimate.api.content.IContentType;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.data.DataDisplay;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Independent name and title bindings, carried by Custom NPCs' normal display save and synchronization. */
@Getter
@Setter
public final class NpcDisplayNames
{
    private static final String NBT_NAME = "WolffsModNameBinding";
    private static final String NBT_TITLE = "WolffsModTitleBinding";

    public interface Holder
    {
        NpcDisplayNames wolffsmodnpcsDisplayNames();

        void wolffsmodnpcsNamesChanged();
    }

    @Nullable
    private ContentNameBinding name;
    @Nullable
    private ContentNameBinding title;

    public static NpcDisplayNames of(DataDisplay display)
    {
        return ((Holder) display).wolffsmodnpcsDisplayNames();
    }

    public static void literalName(DataDisplay display, String name)
    {
        display.setName(name);
        of(display).name = null;
        ((Holder) display).wolffsmodnpcsNamesChanged();
    }

    public static void literalTitle(DataDisplay display, String title)
    {
        display.setTitle(title);
        of(display).title = null;
        ((Holder) display).wolffsmodnpcsNamesChanged();
    }

    public static void bindName(DataDisplay display, IContentType type)
    {
        display.setName(type.getName());
        of(display).name = ContentNameBinding.of(type);
        ((Holder) display).wolffsmodnpcsNamesChanged();
    }

    public static void bindTitle(DataDisplay display, IContentType type)
    {
        display.setTitle(type.getName());
        of(display).title = ContentNameBinding.of(type);
        ((Holder) display).wolffsmodnpcsNamesChanged();
    }

    public static Component name(DataDisplay display)
    {
        ContentNameBinding binding = of(display).name;
        return binding == null ? Component.literal(display.getName().replace('&', '§')) : binding.component(display.getName());
    }

    public static Component title(DataDisplay display)
    {
        ContentNameBinding binding = of(display).title;
        return binding == null ? Component.literal(display.getTitle()) : binding.component(display.getTitle());
    }

    public void save(CompoundTag tag, String currentName, String currentTitle)
    {
        tag.remove(NBT_NAME);
        tag.remove(NBT_TITLE);
        if (name != null && name.literal().equals(currentName))
            tag.put(NBT_NAME, name.save());
        if (title != null && title.literal().equals(currentTitle))
            tag.put(NBT_TITLE, title.save());
    }

    public void load(CompoundTag tag, String currentName, String currentTitle)
    {
        name = ContentNameBinding.read(tag.getCompound(NBT_NAME), currentName).orElse(null);
        title = ContentNameBinding.read(tag.getCompound(NBT_TITLE), currentTitle).orElse(null);
    }
}
