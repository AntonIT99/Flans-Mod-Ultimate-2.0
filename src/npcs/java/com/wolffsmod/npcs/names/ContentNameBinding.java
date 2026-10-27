package com.wolffsmod.npcs.names;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.teams.ITeam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/** A display-only content reference, with the original literal retained to recognize manual edits. */
public record ContentNameBinding(boolean team, String id, String literal)
{
    private static final String NBT_LITERAL = "literal";

    public static ContentNameBinding of(IContentType type)
    {
        return new ContentNameBinding(type instanceof ITeam, type.getShortName(), type.getName());
    }

    public Component component(String current)
    {
        if (!literal.equals(current))
            return Component.literal(current);
        Optional<? extends IContentType> source = team ? FlansModApi.getTeam(id) : FlansModApi.getPlayerClass(id);
        return source.map(type -> (Component) type.getNameComponent()).orElseGet(() -> Component.literal(current));
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("team", team);
        tag.putString("id", id);
        tag.putString(NBT_LITERAL, literal);
        return tag;
    }

    /** Missing metadata never infers a binding from the text alone. */
    public static Optional<ContentNameBinding> read(CompoundTag tag, String current)
    {
        String id = tag.getString("id");
        String literal = tag.getString(NBT_LITERAL);
        return id.isBlank() || !tag.contains(NBT_LITERAL) || !literal.equals(current) ? Optional.empty() : Optional.of(new ContentNameBinding(tag.getBoolean("team"), id, literal));
    }
}
