package com.flansmodultimate.client.model;

import com.flansmodultimate.common.entity.IFlanEntity;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Builds the models the player is about to see on a background thread, so that the first frame
 * drawing them finds them in {@link ModelCache} instead of stalling on them.
 *
 * <p>
 * Purely an optimization: renderers keep asking {@link ModelCache} as before, and one that gets
 * there first builds the model itself or waits for the build already running. One worker keeps
 * legacy model constructors, which are arbitrary content pack code, from running concurrently.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelPreloader
{
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final long WORKER_IDLE_SECONDS = 30;

    private static final PreloadTracker<InfoType> tracker = new PreloadTracker<>(createWorker(), ModelPreloader::preload);
    private static int ticksUntilScan;

    /**
     * Whether a renderer of this large entity model should skip this frame because the model is
     * still waiting for its background load. Requests the load if nothing has yet. Once the load
     * ended, successfully or not, the renderer returns to asking {@link ModelCache} as usual.
     */
    public static boolean isLoadPending(@Nullable InfoType type)
    {
        request(type);
        return type != null && tracker.isPending(type);
    }

    /** Requests the models of what the local player carries and of what is around them. */
    public static void tick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.getOverlay() != null || --ticksUntilScan > 0)
            return;
        ticksUntilScan = SCAN_INTERVAL_TICKS;

        requestInventory(minecraft.player);
        for (Entity entity : minecraft.level.entitiesForRendering())
        {
            if (entity instanceof IFlanEntity<?> flanEntity)
                request(flanEntity.getConfigType());
            if (entity instanceof LivingEntity living)
                living.getAllSlots().forEach(ModelPreloader::requestStack);
        }
    }

    /** Forgets what was requested, so that it is requested again for the reloaded cache. */
    static void clear()
    {
        tracker.clear();
    }

    private static void requestInventory(LocalPlayer player)
    {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            requestStack(inventory.getItem(slot));
    }

    private static void requestStack(ItemStack stack)
    {
        if (stack.getItem() instanceof IFlanItem<?> flanItem)
            request(flanItem.getConfigType());
        if (stack.getItem() instanceof GunItem gunItem)
        {
            for (AttachmentType attachment : gunItem.getConfigType().getCurrentAttachments(stack))
                request(attachment);
        }
    }

    private static void request(@Nullable InfoType type)
    {
        if (type != null)
            tracker.request(type);
    }

    private static void preload(InfoType type)
    {
        try
        {
            ModelCache.loadModelsOf(type);
        }
        catch (RuntimeException | LinkageError e)
        {
            // The renderer that needs this model tries again and reports the failure where it always did.
            FlansLog.log.debug("Could not preload the models of {}", type, e);
        }
    }

    private static ThreadPoolExecutor createWorker()
    {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, WORKER_IDLE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), task ->
        {
            Thread thread = new Thread(task, "Flan's Mod Model Preloader");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            // Model classes compiled into the mod are looked up through the context class loader.
            thread.setContextClassLoader(ModelPreloader.class.getClassLoader());
            return thread;
        });
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
