package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.teams.editor.TeamEditorService;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

/** Operator intent only: permission, schema and revision checks happen on the server. */
@NoArgsConstructor
public final class PacketEditorAction implements IServerPacket
{
    private String operation = "open";
    private boolean team;
    private String key = "";
    private String json = "{}";
    private long revision;

    public PacketEditorAction(String operation, boolean team, String key, JsonObject values, long revision)
    {
        this.operation = operation;
        this.team = team;
        this.key = key;
        json = values.toString();
        this.revision = revision;
    }

    public static PacketEditorAction open()
    {
        return new PacketEditorAction();
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUtf(operation, 16);
        data.writeBoolean(team);
        data.writeUtf(key, 512);
        data.writeUtf(json, 262144);
        data.writeLong(revision);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        operation = data.readUtf(16);
        team = data.readBoolean();
        key = data.readUtf(512);
        json = data.readUtf(262144);
        revision = data.readLong();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!com.flansmodultimate.common.teams.editor.EditorPermissions.mayEdit(player))
        {
            player.sendSystemMessage(Component.literal("The teams editor requires operator level 2 or flansmodultimate.teams.editor permission."));
            return;
        }
        TeamEditorService service = TeamEditorService.get();
        try
        {
            if ("open".equals(operation))
            {
                String screen = team ? "team" : "class";
                if (key.isEmpty())
                    screen = "list";
                service.sync(player, screen, key, "");
            }
            else
            {
                service.edit(team, key, operation, JsonParser.parseString(json).getAsJsonObject(), revision);
                service.sync(player, team ? "teams" : "classes", "", "Saved. Equipment and skins take effect on the next spawn.");
            }
        }
        catch (IOException | RuntimeException exception)
        {
            service.sync(player, "", "", "Save failed: " + exception.getMessage());
        }
    }
}
