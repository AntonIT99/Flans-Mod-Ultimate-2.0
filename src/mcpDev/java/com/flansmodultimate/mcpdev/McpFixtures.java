package com.flansmodultimate.mcpdev;

import com.flansmodultimate.apocalyse.client.ApocalypseWorldChoice;
import com.google.gson.JsonObject;
import com.wolffsmod.npcs.client.GuiWolffsMod;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomItems;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.util.concurrent.*;

/** Fixed, disposable fixture isolated from ordinary development saves. */
final class McpFixtures
{
    private static final String WORLD = "mcp-ui-fixture";
    private static final String NPC_TAG = "flans_mcp_fixture";

    private McpFixtures()
    {}

    static void requireProfile()
    {
        McpUi.requireControl();
        if (!Boolean.getBoolean("fmu.mcp.testProfile"))
            throw new IllegalStateException("Fixture operations require -PmcpTestProfile");
    }

    static JsonObject world() throws IOException
    {
        requireProfile();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null || minecraft.getSingleplayerServer() != null)
            throw new IllegalStateException("Return to title before opening the fixture");
        minecraft.options.pauseOnLostFocus = false;
        if (minecraft.getLevelSource().levelExists(WORLD))
        {
            if (ApocalypseWorldChoice.needsChoice(minecraft, WORLD))
                ApocalypseWorldChoice.record(minecraft, WORLD, false);
            minecraft.createWorldOpenFlows().loadLevel(minecraft.screen, WORLD);
        }
        else
        {
            LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(12345L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }
        JsonObject result = new JsonObject();
        result.addProperty("opening_world", WORLD);
        return result;
    }

    // Called on the HTTP thread: never block the client waiting for server work.
    static JsonObject editor() throws InterruptedException, ExecutionException, TimeoutException
    {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.submit(() ->
        {
            requireProfile();
            var integrated = minecraft.getSingleplayerServer();
            if (integrated == null || !WORLD.equals(integrated.getWorldData().getLevelName()))
                throw new IllegalStateException("Open the isolated fixture world first");
            return integrated;
        }).get(10, TimeUnit.SECONDS);
        int id = server.submit(() ->
        {
            var player = server.getPlayerList().getPlayer(minecraft.getUser().getGameProfile().getId());
            if (player == null)
                throw new IllegalStateException("Fixture player is not ready");
            EntityCustomNpc fixture = prepareNpc(player);
            NoppesUtilServer.setEditingNpc(player, fixture);
            return fixture.getId();
        }).get(10, TimeUnit.SECONDS);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline)
        {
            boolean opened = minecraft.submit(() ->
            {
                requireProfile();
                if (minecraft.level == null || !(minecraft.level.getEntity(id) instanceof EntityNPCInterface npc))
                    return false;
                NoppesUtil.openGUI(minecraft.player, new GuiWolffsMod(npc));
                return true;
            }).get(10, TimeUnit.SECONDS);
            if (opened)
            {
                JsonObject result = new JsonObject();
                result.addProperty("npc_id", id);
                result.addProperty("opened", "GuiWolffsMod");
                return result;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        throw new IllegalStateException("Fixture NPC did not reach the client within 5 seconds");
    }

    private static EntityCustomNpc prepareNpc(ServerPlayer player)
    {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CustomItems.wand));
        player.inventoryMenu.broadcastChanges();
        for (var entity : player.serverLevel().getAllEntities())
            if (entity instanceof EntityCustomNpc npc && entity.getTags().contains(NPC_TAG))
                return npc;
        EntityCustomNpc fixture = new EntityCustomNpc(CustomEntities.entityCustomNpc, player.serverLevel());
        fixture.addTag(NPC_TAG);
        fixture.setCustomName(Component.literal("MCP editor fixture"));
        fixture.setNoAi(true);
        fixture.moveTo(player.getX() + 2, player.getY(), player.getZ());
        player.serverLevel().addFreshEntity(fixture);
        return fixture;
    }
}
