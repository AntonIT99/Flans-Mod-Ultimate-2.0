package com.flansmodultimate.client.distant;

import com.flansmodultimate.client.distant.dh.DhDistantTerrain;
import com.flansmodultimate.common.distant.DistantRenderRange;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketDistantSubscription;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Entry point of the optional Distant Horizons integration on the client. Distant Horizons is only touched
 * through {@link IDistantTerrain}, whose implementation is loaded once the mod is known to be present, so
 * everything here runs unchanged without it. Render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantHorizonsClient
{
    public static final String MOD_ID = "distanthorizons";
    /** The vanilla chunks end about here short of the render distance, where the far terrain takes over. */
    private static final double HANDOFF_MARGIN = 16D;
    private static final double MIN_HANDOFF_DISTANCE = 64D;

    private static IDistantTerrain terrain = IDistantTerrain.NONE;
    /** What the server was last asked to send on this connection, or null before asking. */
    @Nullable
    private static Boolean subscribedContacts;
    @Nullable
    private static Boolean subscribedExplosions;

    /** Looks for Distant Horizons once, during client setup. */
    public static void init()
    {
        if (!PlatformEnvironment.isModLoaded(MOD_ID))
            return;
        try
        {
            terrain = DhDistantTerrain.create();
        }
        catch (LinkageError | RuntimeException exception)
        {
            FlansLog.log.warn("Could not hook into Distant Horizons; Flan's Mod far-terrain integration stays off", exception);
            terrain = IDistantTerrain.NONE;
        }
    }

    /** Whether Distant Horizons is installed, compatible and enabled in the client settings. */
    public static boolean enabled()
    {
        ModClientConfig config = ModClientConfig.get();
        return terrain != IDistantTerrain.NONE && config != null && config.distantHorizonsIntegration;
    }

    /** The far-terrain renderer to use, or {@link IDistantTerrain#NONE} while the integration is off. */
    public static IDistantTerrain terrain()
    {
        return enabled() ? terrain : IDistantTerrain.NONE;
    }

    public static boolean rangefinderEnabled()
    {
        return enabled() && ModClientConfig.get().distantHorizonsRangefinder;
    }

    public static boolean contactsEnabled()
    {
        return enabled() && ModClientConfig.get().distantHorizonsContacts;
    }

    public static boolean explosionsEnabled()
    {
        return enabled() && ModClientConfig.get().distantHorizonsExplosions;
    }

    /**
     * Camera distance in blocks from which shapes are left to the far-terrain renderer. Nearer, vanilla
     * terrain is drawn over whatever the far-terrain renderer drew, so shapes there are drawn by the mod.
     */
    public static double handoffDistance()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return Math.max(MIN_HANDOFF_DISTANCE, minecraft.options.getEffectiveRenderDistance() * 16D - HANDOFF_MARGIN);
    }

    /** Runs at the end of every client tick. */
    public static void tick()
    {
        terrain.tick();
        Minecraft minecraft = Minecraft.getInstance();
        updateSubscription(minecraft);

        IDistantTerrain active = terrain();
        ModClientConfig config = ModClientConfig.get();
        boolean extend = active.drawsBoxes() && config != null && config.distantHorizonsDriveableRendering;
        double handoff = handoffDistance();
        DistantRenderRange.setDriveableRenderDistance(extend ? handoff : 0D);

        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null)
            return;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        DistantContactsClient.tick(level, camera, active, contactsEnabled(), handoff);
        DistantExplosionCues.tick(camera, active, explosionsEnabled(), handoff);
        DistantRangefinder.poll();
    }

    /** Tells the server what to send whenever that changes, and once on every new connection. */
    private static void updateSubscription(Minecraft minecraft)
    {
        if (minecraft.getConnection() == null || minecraft.player == null)
            return;

        boolean contacts = contactsEnabled();
        boolean explosions = explosionsEnabled();
        // Without Distant Horizons there is nothing to ask for, and no reason to send anything
        if (subscribedContacts == null && !contacts && !explosions)
            return;
        if (Boolean.valueOf(contacts).equals(subscribedContacts) && Boolean.valueOf(explosions).equals(subscribedExplosions))
            return;

        PacketHandler.sendToServer(new PacketDistantSubscription(contacts, explosions));
        subscribedContacts = contacts;
        subscribedExplosions = explosions;
    }

    /** Forgets the world on disconnecting. */
    public static void reset()
    {
        subscribedContacts = null;
        subscribedExplosions = null;
        DistantRenderRange.setDriveableRenderDistance(0D);
        DistantContactsClient.reset();
        DistantExplosionCues.reset();
        DistantRangefinder.reset();
        DistantTextureColors.clear();
        terrain.reset();
    }
}
