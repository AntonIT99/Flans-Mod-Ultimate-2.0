package com.flansmodultimate.platform.client;

import com.flansmodultimate.mixin.ModMismatchDisconnectedScreenAccessor;
import com.flansmodultimate.platform.network.ContentPackDiagnostics;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.client.gui.ModMismatchDisconnectedScreen;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Map;
import java.util.Set;

/** Handles failed logins before Forge initializes its metadata-dependent mismatch table. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentMismatchScreens
{
    private static final Set<String> CONTENT_NAMESPACES = Set.of("flansmod", "flansmodultimate", "wolffsmodnpcs");

    public static Screen safeScreen(Screen screen)
    {
        if (!(screen instanceof ModMismatchDisconnectedScreen) || !(screen instanceof ModMismatchDisconnectedScreenAccessor context))
            return screen;

        var data = context.flansmodultimateMismatchData();
        boolean contentMismatch = data.mismatchedModData().keySet().stream().anyMatch(id -> id.getPath().isEmpty() && CONTENT_NAMESPACES.contains(id.getNamespace()));
        boolean missingMetadata = data.mismatchedModData().keySet().stream().anyMatch(id -> !data.presentModData().containsKey(id));
        if (!contentMismatch && !missingMetadata)
            return screen;

        // Legacy registry namespaces such as flansmod are not mod ids. Forge dereferences their absent metadata in init().
        // Keep the disconnect and its original parent, using vanilla's screen instead of constructing that table.
        FlansLog.log.warn("Login rejected: {}. Mismatched namespaces/channels: {}", context.flansmodultimateReason().getString(), data.mismatchedModData().keySet());
        Component original = context.flansmodultimateReason();
        Component reason;
        if (original.getContents() instanceof TranslatableContents translation && translation.getKey().equals("disconnect.flansmodultimate.content_mismatch"))
            reason = original;
        else if (contentMismatch)
            reason = ContentPackDiagnostics.message(Map.of());
        else
            reason = Component.translatable("disconnect.flansmodultimate.registry_mismatch");
        return new DisconnectedScreen(context.flansmodultimateParent(), screen.getTitle(), reason);
    }
}
