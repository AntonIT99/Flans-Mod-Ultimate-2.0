package com.flansmodultimate.platform.client;

import com.flansmodultimate.mixin.ModMismatchDisconnectedScreenAccessor;
import net.minecraftforge.client.gui.ModMismatchDisconnectedScreen;
import net.minecraftforge.network.ConnectionData.ModMismatchData;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentMismatchScreensTest
{
    @Test
    void legacyRegistryNamespaceWithoutModMetadataDisconnectsSafely()
    {
        var screen = mismatch("flansmod", "", Map.of());
        assertInstanceOf(DisconnectedScreen.class, ContentMismatchScreens.safeScreen(screen));
    }

    @Test
    void missingNpcModelsUseContentMessageEvenWithValidModMetadata()
    {
        var id = ResourceLocation.fromNamespaceAndPath("wolffsmodnpcs", "");
        var screen = mismatch(id.getNamespace(), id.getPath(), Map.of(id, Pair.of("NPC Vehicles & Soldiers", "1.0")));
        assertInstanceOf(DisconnectedScreen.class, ContentMismatchScreens.safeScreen(screen));
    }

    @Test
    void otherMissingModMetadataAlsoDisconnectsSafely()
    {
        assertInstanceOf(DisconnectedScreen.class, ContentMismatchScreens.safeScreen(mismatch("othermod", "", Map.of())));
    }

    @Test
    void ordinaryChannelMismatchKeepsForgeDetails()
    {
        var id = ResourceLocation.fromNamespaceAndPath("flansmodultimate", "main");
        var screen = mismatch(id.getNamespace(), id.getPath(), Map.of(id, Pair.of("Flan's Mod Ultimate", "2.2")));
        assertSame(screen, ContentMismatchScreens.safeScreen(screen));
    }

    @Test
    void unrelatedScreensAndClosingScreenAreUnchanged()
    {
        Screen screen = mock(Screen.class);
        assertSame(screen, ContentMismatchScreens.safeScreen(screen));
        assertNull(ContentMismatchScreens.safeScreen(null));
    }

    private static Screen mismatch(String namespace, String path, Map<ResourceLocation, Pair<String, String>> present)
    {
        var screen = mock(ModMismatchDisconnectedScreen.class, withSettings().extraInterfaces(ModMismatchDisconnectedScreenAccessor.class));
        var context = (ModMismatchDisconnectedScreenAccessor) screen;
        when(context.flansmodultimateMismatchData()).thenReturn(new ModMismatchData(Map.of(ResourceLocation.fromNamespaceAndPath(namespace, path), "ABSENT"), present, false));
        when(context.flansmodultimateReason()).thenReturn(Component.literal("Failed to synchronize registry data from server"));
        Screen parent = mock(Screen.class);
        when(context.flansmodultimateParent()).thenReturn(parent);
        when(screen.getTitle()).thenReturn(Component.literal("Connection lost"));
        return screen;
    }
}
