package com.flansmodultimate.mixin;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseDatapackSource;
import com.flansmodultimate.apocalyse.client.ApocalypseWorldChoice;
import com.flansmodultimate.apocalyse.client.ApocalypseWorldChoiceScreen;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.WorldDataConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Makes the player decide whether a new world gets the Apocalypse dimension before it is created.
 *
 * <p>Creating the world is held back until the player answers. The answer is applied through the
 * same data pack reload the Data Packs screen uses, so the world is validated and created exactly
 * as if the player had moved the Apocalypse pack there by hand.</p>
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenApocalypseMixin extends Screen
{
    @Shadow
    public abstract WorldCreationUiState getUiState();

    /** Set while re-entering {@code onCreate} after the player has answered. */
    @Unique
    private boolean flansmodultimate$answered;

    /** The answer waiting for its data pack reload to finish, or null. */
    @Unique
    @Nullable
    private Boolean flansmodultimate$pendingApocalypse;

    protected CreateWorldScreenApocalypseMixin(Component title)
    {
        super(title);
    }

    @Shadow
    private void onCreate()
    {
    }

    @Shadow
    @Nullable
    private Pair<Path, PackRepository> getDataPackSelectionSettings(WorldDataConfiguration configuration)
    {
        return null;
    }

    @Shadow
    private void tryApplyNewDataPacks(PackRepository repository, boolean shouldConfirm, Consumer<WorldDataConfiguration> callback)
    {
    }

    @Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$askAboutApocalypse(CallbackInfo callback)
    {
        if (flansmodultimate$answered)
        {
            flansmodultimate$answered = false;
            return;
        }
        if (!ApocalypseWorldChoice.isOffered(getUiState().getSettings().dataConfiguration()))
            return;
        callback.cancel();
        Screen createScreen = this;
        minecraft.setScreen(ApocalypseWorldChoiceScreen.forNewWorld(this::flansmodultimate$applyChoice,
            () -> minecraft.setScreen(createScreen)));
    }

    /**
     * Once the data packs have reloaded with the player's answer, carries on creating the world.
     * The reload returns to this screen without initialising it again, so this waits for the
     * screen's first render after it is shown once more.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void flansmodultimate$resumeAfterReload(GuiGraphics graphics, int mouseX, int mouseY,
                                                     float partialTick, CallbackInfo callback)
    {
        Boolean pending = flansmodultimate$pendingApocalypse;
        if (pending == null)
            return;
        flansmodultimate$pendingApocalypse = null;
        // A failed reload returns here with the old packs: the player is back on this screen
        // to try again, and nothing is created with a configuration they did not choose.
        // Created outside the screen tick, as a click on Create would be.
        if (ApocalypseWorldChoice.isEnabled(getUiState().getSettings().dataConfiguration()) == pending)
            minecraft.tell(this::flansmodultimate$createAnswered);
    }

    @Unique
    private void flansmodultimate$applyChoice(boolean withApocalypse)
    {
        Screen createScreen = this;
        WorldDataConfiguration configuration = getUiState().getSettings().dataConfiguration();
        if (ApocalypseWorldChoice.isEnabled(configuration) == withApocalypse)
        {
            minecraft.setScreen(createScreen);
            flansmodultimate$createAnswered();
            return;
        }

        Pair<Path, PackRepository> selection = getDataPackSelectionSettings(configuration);
        if (selection == null)
        {
            FlansMod.log.warn("Could not prepare the data packs to {} the Apocalypse dimension", withApocalypse ? "add" : "remove");
            minecraft.setScreen(createScreen);
            return;
        }
        PackRepository repository = selection.getSecond();
        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        selected.remove(ApocalypseDatapackSource.PACK_ID);
        if (withApocalypse)
            selected.add(ApocalypseDatapackSource.PACK_ID);
        repository.setSelected(selected);
        flansmodultimate$pendingApocalypse = withApocalypse;
        tryApplyNewDataPacks(repository, false, ignored -> minecraft.setScreen(createScreen));
    }

    @Unique
    private void flansmodultimate$createAnswered()
    {
        flansmodultimate$answered = true;
        onCreate();
    }
}
