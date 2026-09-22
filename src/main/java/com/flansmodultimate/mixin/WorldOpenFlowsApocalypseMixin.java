package com.flansmodultimate.mixin;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.client.ApocalypseWorldChoice;
import com.flansmodultimate.apocalyse.client.ApocalypseWorldChoiceScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.network.chat.Component;

import java.io.IOException;

/**
 * Asks once, before a saved world without the Apocalypse dimension is opened, whether to add it.
 * Every way of opening a saved world from the client, the world list and quick play alike, goes
 * through {@code loadLevel}.
 */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsApocalypseMixin
{
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "loadLevel(Lnet/minecraft/client/gui/screens/Screen;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$askAboutApocalypse(Screen lastScreen, String levelId, CallbackInfo callback)
    {
        if (!ApocalypseWorldChoice.needsChoice(minecraft, levelId))
            return;
        callback.cancel();
        minecraft.setScreen(ApocalypseWorldChoiceScreen.forExistingWorld(
            withApocalypse -> flansmodultimate$recordAndLoad(lastScreen, levelId, withApocalypse),
            () -> minecraft.setScreen(lastScreen)));
    }

    @Unique
    private void flansmodultimate$recordAndLoad(Screen lastScreen, String levelId, boolean withApocalypse)
    {
        try
        {
            ApocalypseWorldChoice.record(minecraft, levelId, withApocalypse);
        }
        catch (IOException | RuntimeException exception)
        {
            FlansMod.log.error("Could not record the Apocalypse choice for world '{}'", levelId, exception);
            minecraft.setScreen(new AlertScreen(() -> minecraft.setScreen(lastScreen),
                Component.translatable("gui.flansmodultimate.apocalypse_choice.title"),
                Component.translatable("gui.flansmodultimate.apocalypse_choice.save_failed", exception.getMessage())));
            return;
        }
        // The choice is now in the world's data pack lists, so this load is not asked about again.
        ((WorldOpenFlows) (Object) this).loadLevel(lastScreen, levelId);
    }
}
